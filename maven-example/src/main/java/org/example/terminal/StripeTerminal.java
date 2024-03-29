package org.example.terminal;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.appinfo.ApplicationInformation;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.callable.ReaderCallback;
import com.stripe.stripeterminal.external.callable.ReadersCallback;
import com.stripe.stripeterminal.external.callable.RefundCallback;
import com.stripe.stripeterminal.external.models.*;
import com.stripe.stripeterminal.log.LogLevel;
import org.example.AppUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import static com.stripe.model.StripeObject.PRETTY_PRINT_GSON;

/** Wrapper class for the Terminal object. */
public class StripeTerminal implements IStripeTerminal{
  private final @NotNull Listener listener;
  public StripeTerminal() {
    String appName = "org.example.ugoTestCliMavenApp";
    listener = new Listener();
    if (!Terminal.isInitialized()) {
      Terminal.initTerminal(
          new TokenProvider(),
          listener,
          new ApplicationInformation(appName, "2.0.0", AppUtils.appDataDirectory(appName)),
          LogLevel.INFO,
          listener);
    }
  }

  @Override
  public @Nullable ConnectionStatus getConnectionStatus() {
    return listener.getConnectionStatus();
  }

  @Override
  public @Nullable PaymentStatus getPaymentStatus() {
    return listener.getPaymentStatus();
  }

  @Override
  public @Nullable OfflineStatus getOfflineStatus() {
    return listener.getOfflineStatus();
  }

  public CompletableFuture<List<Reader>> discoverReaders(boolean simulated) {
    CompletableFuture<List<Reader>> f = new CompletableFuture<>();

    if (simulated) {
      long simulatedFixedTipAmount = 1000L;
      SimulatorConfiguration simulatorConfig =
          new SimulatorConfiguration(
              new SimulatedCard(SimulatedCardType.AMEX), simulatedFixedTipAmount);
      Terminal.getInstance().setSimulatorConfiguration(simulatorConfig);
    }

    long startTime = System.nanoTime();
    Terminal.getInstance()
        .discoverReaders(
            new DiscoveryConfiguration(simulated, null, 3),
            new ReadersCallback() {
              @Override
              public void onSuccess(@NotNull List<Reader> list) {
                // complete future with the discovered readers
                f.complete(list);
                System.out.println(
                    "Time elapsed = "
                        + TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime));
              }

              @Override
              public void onFailure(@NotNull TerminalException e) {
                // complete future with the error
                f.completeExceptionally(e);
              }
            });
    return f;
  }

  public CompletableFuture<Reader> connectReader(@NotNull Reader reader) {
    CompletableFuture<Reader> f = new CompletableFuture<>();

    Terminal.getInstance()
        .connectInternetReader(
            reader,
            new InternetConnectionConfiguration(/*fail_if_in_use*/ true),
            new ReaderCallback() {
              @Override
              public void onSuccess(@NotNull Reader reader) {
                f.complete(reader);
              }

              @Override
              public void onFailure(@NotNull TerminalException e) {
                f.completeExceptionally(e);
              }
            });

    return f;
  }

  public void disconnectReader() {
    VoidFuture f = new VoidFuture();
    Terminal.getInstance().disconnectReader(f);
    f.join();
  }

  // region Display Cart
  public void clearReaderDisplay() {
    VoidFuture f = new VoidFuture();
    Terminal.getInstance().clearReaderDisplay(f);
    f.join();
  }

  public void displayCart(@NotNull String currency) {
    VoidFuture f = new VoidFuture();
    List<CartLineItem> lineItems = new ArrayList<>();
    lineItems.add(new CartLineItem.Builder("box of donuts", 1, 2000).build());
    lineItems.add(new CartLineItem.Builder("coffee", 1, 8000).build());

    Cart.Builder cartBuilder = new Cart.Builder(currency, 1500, 11500);
    cartBuilder.setLineItems(lineItems);
    Terminal.getInstance().setReaderDisplay(cartBuilder.build(), f);
    f.join();
  }

  public void displayCart(@NotNull Cart cart) {
    VoidFuture f = new VoidFuture();
    Terminal.getInstance().setReaderDisplay(cart, f);
    f.join();
  }
  // endregion Display Cart

  // region Take Payment
  private CompletableFuture<PaymentIntent> createPayment(@NotNull String currency) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    List<PaymentMethodType> paymentMethodTypes = new ArrayList<>();
    System.out.print("Enter amount in cents: ");
    Scanner scanner = new Scanner(System.in);
    long amount = scanner.nextLong();
    paymentMethodTypes.add(PaymentMethodType.CARD_PRESENT);
    if (currency.equals("cad")) { // add Interac as a payment method for canadian dollars
      paymentMethodTypes.add(PaymentMethodType.INTERAC_PRESENT);
    }
    PaymentIntentParameters parameters =
        new PaymentIntentParameters.Builder(
                amount, currency, CaptureMethod.Automatic, paymentMethodTypes)
            // set metadata to identify the payment  when it is forwarded
            .setMetadata(
                Map.of("store-payment-id", UUID.randomUUID().toString(), "key", "ugo-offline"))
            .build();
    CreateConfiguration configuration = new CreateConfiguration(getOfflineBehavior(amount));
    Terminal.getInstance().createPaymentIntent(parameters, configuration, f);
    return f;
  }

  public CompletableFuture<PaymentIntent> createPayment(@NotNull PaymentIntentParameters parameters, @NotNull CreateConfiguration configuration) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    Terminal.getInstance().createPaymentIntent(parameters, configuration, f);
    return f;
  }

  /**
   * Force small ticket items to be processed offline for line busting, and prevent large ticket
   * items from being processed offline.
   *
   * @param amount - cart total
   * @return {@link OfflineBehavior} to use for this transaction
   */
  private @NotNull OfflineBehavior getOfflineBehavior(Long amount) {
    if (amount < 50_00L) {
      // Item amount is small force it to happen offline, so we can move on quickly to other sales
      return OfflineBehavior.FORCE_OFFLINE;
    } else if (amount > 1000_00L) {
      // Item amount is quite large lets make sure we're collecting offline, so we don't risk
      // offline declines
      return OfflineBehavior.REQUIRE_ONLINE;
    } else {
      // Otherwise prefer this to happen offline, and fallback to offline if it fails
      return OfflineBehavior.PREFER_ONLINE;
    }
  }

  public CompletableFuture<PaymentIntent> collectPaymentMethod(
      @NotNull PaymentIntent paymentIntent) {
    PaymentMethod pm = paymentIntent.getPaymentMethod();
    assert pm != null;
    CardPresentDetails card = pm.getCardPresentDetails() != null ? pm.getCardPresentDetails()
            : pm.getInteracPresentDetails();
    PaymentIntentFuture f = new PaymentIntentFuture();
    // Add
    CollectConfiguration config = new CollectConfiguration.Builder()
            .updatePaymentIntent(true)
            .build();
    Cancelable cancelable = Terminal.getInstance().collectPaymentMethod(paymentIntent, config, f);
    return f;
  }

  private CompletableFuture<PaymentIntent> retrievePaymentIntent(@NotNull String clientSecret) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    Terminal.getInstance().retrievePaymentIntent(clientSecret, f);
    return f;
  }

  CompletableFuture<PaymentIntent> confirmPaymentIntent(
      @NotNull PaymentIntent paymentIntent) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    Terminal.getInstance().confirmPaymentIntent(paymentIntent, f);
    return f;
  }

  public PaymentIntent takePaymentClientSideCreate(@NotNull String currency)
      throws ExecutionException, InterruptedException {
    PaymentIntent paymentIntent;
    paymentIntent = createPayment(currency).get();
    System.out.print("Created PI: ");
    prettyPrint(paymentIntent);
    paymentIntent = collectPaymentMethod(paymentIntent).get();
    paymentIntent = confirmPaymentIntent(paymentIntent).get();
    System.out.print("Confirmed PI: ");
    prettyPrint(paymentIntent);
    return paymentIntent;
  }

  /**
   * Take a payment created using the Merchant's BE on the terminal, by retrieving the
   * PaymentIntent, attaching a payment method and confirming the payment intent.
   *
   * @param clientSecret - Client secret on the Payment Intent object retrieved from the Merchant's
   *     BE
   * @return Processed {@link PaymentIntent}
   * @throws ExecutionException from {@link CompletableFuture#get}
   * @throws InterruptedException from {@link CompletableFuture#get}
   */
  public PaymentIntent takePaymentServerSideCreate(@NotNull String clientSecret)
      throws ExecutionException, InterruptedException {
    PaymentIntent paymentIntent;
    paymentIntent = retrievePaymentIntent(clientSecret).get();
    paymentIntent = collectPaymentMethod(paymentIntent).get();
    paymentIntent = confirmPaymentIntent(paymentIntent).get();
    return paymentIntent;
  }
  // endregion Take Payment

  // region Save Card

  public SetupIntent saveCardClientSideCreate() throws ExecutionException, InterruptedException {
    SetupIntent setupIntent;
    setupIntent = createSetupIntent().get();
    setupIntent = collectSetupPaymentMethod(setupIntent, null).get();
    setupIntent = confirmSetupIntent(setupIntent).get();
    return setupIntent;
  }

  public SetupIntent saveCardServerSideCreate(@NotNull String secret)
      throws ExecutionException, InterruptedException {
    SetupIntent setupIntent;
    setupIntent = retrieveSetupIntent(secret).get();
    setupIntent = collectSetupPaymentMethod(setupIntent, null).get();
    setupIntent = confirmSetupIntent(setupIntent).get();
    return setupIntent;
  }

  private CompletableFuture<SetupIntent> createSetupIntent() {
    return createSetupIntent(SetupIntentParameters.Companion.getNULL());
  }

  private CompletableFuture<SetupIntent> createSetupIntent(@NotNull SetupIntentParameters parameters) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance().createSetupIntent(parameters, f);
    return f;
  }

  private CompletableFuture<SetupIntent> retrieveSetupIntent(@NotNull String clientSecret) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance().retrieveSetupIntent(clientSecret, f);
    return f;
  }

  private CompletableFuture<SetupIntent> collectSetupPaymentMethod(
      @NotNull SetupIntent setupIntent,
      @Nullable SetupIntentConfiguration configuration
  ) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance()
        .collectSetupIntentPaymentMethod(setupIntent, /*customerConsentCollected */ true, configuration, f);
    return f;
  }

  private CompletableFuture<SetupIntent> confirmSetupIntent(@NotNull SetupIntent setupIntent) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance().confirmSetupIntent(setupIntent, f);
    return f;
  }

  // endregion Save Card

  public Refund refund(@NotNull String chargeId, @NotNull String currency, long amount)
      throws ExecutionException, InterruptedException {
    VoidFuture collectRefundFuture = new VoidFuture();
    CompletableFuture<Refund> processRefundFuture = new CompletableFuture<>();
    RefundParameters parameters = new RefundParameters.Builder(chargeId, amount, currency).build();
    Terminal.getInstance().collectRefundPaymentMethod(parameters, collectRefundFuture);
    collectRefundFuture.get();
    Terminal.getInstance()
        .confirmRefund(
            new RefundCallback() {
              @Override
              public void onSuccess(@NotNull Refund refund) {
                processRefundFuture.complete(refund);
              }

              @Override
              public void onFailure(@NotNull TerminalException e) {
                processRefundFuture.completeExceptionally(e);
              }
            });
    return processRefundFuture.get();
  }

  // region Collect Inputs
  @Override
  public List<? extends CollectInputsResult> collectInputs(
      @NotNull CollectInputsParameters parameters) throws Throwable {
    CollectInputsFuture f = new CollectInputsFuture();
    Cancelable c = Terminal.getInstance().collectInputs(parameters, f);
    return getOrThrow(f);
  }

  // endregion Collect Inputs

  public void printOfflineStatus() {
    prettyPrint(Terminal.getInstance().getOfflineStatus());
  }

  /**
   * Prints a beautified string of the object using GSON.
   *
   * @param object to beautify and print
   */
  private void prettyPrint(@Nullable Object object) {
    System.out.println(PRETTY_PRINT_GSON.toJson(object));
    if (object instanceof PaymentIntent) {
      System.out.print("Offline Details: ");
      prettyPrint(((PaymentIntent) object).getOfflineDetails());
    }
  }

  private <T> T getOrThrow(CompletableFuture<T> future) throws Throwable {
    T obj;
    try {
      obj = future.get();
    } catch (ExecutionException e) {
      throw e.getCause();
    }
    return obj;
  }
  @Override
  public @NotNull Terminal getTerminal() {
    return Terminal.getInstance();
  }

  @Override
  public @NotNull VoidFuture waitForOfflineStatus(Predicate<OfflineStatus> predicate) {
    return listener.waitForOfflineStatus(predicate);
  }

  @Override
  public @NotNull VoidFuture waitFor(BiPredicate<PaymentStatus, ConnectionStatus> predicate) {
    return listener.waitFor(predicate);
  }

  @Override
  public @NotNull VoidFuture waitForForwarding(BiPredicate<PaymentIntent, TerminalException> predicate) {
    return listener.waitForForwarding(predicate);
  }
}
