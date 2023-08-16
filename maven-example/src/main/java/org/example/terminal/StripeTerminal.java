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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.stripe.model.StripeObject.PRETTY_PRINT_GSON;

/** Wrapper class for the Terminal object. */
public class StripeTerminal {
  public StripeTerminal() {
    String appName = "org.example.ugoTestCliMavenApp";
    if (!Terminal.isInitialized()) {
      Terminal.initTerminal(
              new TokenProvider(),
              new Listener(),
              new ApplicationInformation(appName, "1.0.0", AppUtils.appDataDirectory(appName)),
              LogLevel.INFO
      );
    }
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

    Terminal.getInstance()
        .discoverReaders(
            new DiscoveryConfiguration(simulated),
            new ReadersCallback() {
              @Override
              public void onSuccess(@NotNull List<Reader> list) {
                // complete future with the discovered readers
                f.complete(list);
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

  public void disconnectReader() throws ExecutionException, InterruptedException {
    VoidFuture f = new VoidFuture();
    Terminal.getInstance().disconnectReader(f);
    f.get();
  }

  //region Display Cart
  public void clearReaderDisplay() throws ExecutionException, InterruptedException {
    VoidFuture f = new VoidFuture();
    Terminal.getInstance().clearReaderDisplay(f);
    f.get();
  }
  public void displayCart(@NotNull String currency) throws ExecutionException, InterruptedException {
    VoidFuture f = new VoidFuture();
    List<CartLineItem> lineItems = new ArrayList<>();
    lineItems.add(new CartLineItem.Builder("box of donuts", 1, 2000).build());
    lineItems.add(new CartLineItem.Builder("coffee", 1, 8000).build());

    Cart.Builder cartBuilder = new Cart.Builder(currency, 1500, 11500);
    cartBuilder.setLineItems(lineItems);
    Terminal.getInstance().setReaderDisplay(cartBuilder.build(), f);
    f.get();
  }
  //endregion Display Cart

  // region Take Payment
  private CompletableFuture<PaymentIntent> createPayment(@NotNull String currency) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    List<PaymentMethodType> paymentMethodTypes = new ArrayList<>();
    paymentMethodTypes.add(PaymentMethodType.CARD_PRESENT);
    long amount = 10_000L;
    if (currency.equals("cad")) { // add Interac as a payment method for canadian dollars
      paymentMethodTypes.add(PaymentMethodType.INTERAC_PRESENT);
    }
    PaymentIntentParameters parameters =
        new PaymentIntentParameters
                .Builder(amount, currency, CaptureMethod.Automatic, paymentMethodTypes)
                // set metadata to identify the payment  when it is forwarded
                .setMetadata(Map.of("store-payment-id", "very-unique-id"))
                .build();
    CreateConfiguration configuration = new CreateConfiguration(OfflineBehavior.PREFER_ONLINE);
    Terminal.getInstance().createPaymentIntent(parameters, configuration, f);
    return f;
  }

  private CompletableFuture<PaymentIntent> collectPaymentMethod(
      @NotNull PaymentIntent paymentIntent) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    // Add
    CollectConfiguration config = new CollectConfiguration.Builder().setMoto(true).build();
    Cancelable cancelable = Terminal.getInstance().collectPaymentMethod(paymentIntent, config, f);
    return f;
  }

  private CompletableFuture<PaymentIntent> retrievePaymentIntent(@NotNull String clientSecret) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    Terminal.getInstance().retrievePaymentIntent(clientSecret, f);
    return f;
  }

  private CompletableFuture<PaymentIntent> processPaymentIntent(
      @NotNull PaymentIntent paymentIntent) {
    PaymentIntentFuture f = new PaymentIntentFuture();
    Terminal.getInstance().confirmPaymentIntent(paymentIntent, f);
    return f;
  }

  public PaymentIntent takePaymentClientSideCreate(@NotNull String currency)
      throws ExecutionException, InterruptedException {
    PaymentIntent paymentIntent;
    paymentIntent = createPayment(currency).get();
    paymentIntent = collectPaymentMethod(paymentIntent).get();
    paymentIntent = processPaymentIntent(paymentIntent).get();
    return paymentIntent;
  }

  /**
   * Take a payment created using the Merchant's BE on the terminal, by retrieving the PaymentIntent,
   * attaching a payment method and confirming the payment intent.
   * @param clientSecret - Client secret on the Payment Intent object retrieved from the Merchant's BE
   * @return Processed {@link PaymentIntent}
   * @throws ExecutionException from {@link CompletableFuture#get}
   * @throws InterruptedException from {@link CompletableFuture#get}
   */
  public PaymentIntent takePaymentServerSideCreate(@NotNull String clientSecret)
      throws ExecutionException, InterruptedException {
    PaymentIntent paymentIntent;
    paymentIntent = retrievePaymentIntent(clientSecret).get();
    paymentIntent = collectPaymentMethod(paymentIntent).get();
    paymentIntent = processPaymentIntent(paymentIntent).get();
    return paymentIntent;
  }

  public void printOfflineStatus() {
    prettyPrint(Terminal.getInstance().getOfflineStatus());
  }

  /**
   * Prints a beautified string of the object using GSON.
   * @param object to beautify and print
   */
  private void prettyPrint(@Nullable Object object){
    System.out.println(PRETTY_PRINT_GSON.toJson(object));
  }
  // endregion Take Payment

  // region Save Card

  public SetupIntent saveCardClientSideCreate() throws ExecutionException, InterruptedException {
    SetupIntent setupIntent;
    setupIntent = createSetupIntent().get();
    setupIntent = collectSetupPaymentMethod(setupIntent).get();
    setupIntent = confirmSetupIntent(setupIntent).get();
    return setupIntent;
  }

  public SetupIntent saveCardServerSideCreate(@NotNull String secret)
      throws ExecutionException, InterruptedException {
    SetupIntent setupIntent;
    setupIntent = retrieveSetupIntent(secret).get();
    setupIntent = collectSetupPaymentMethod(setupIntent).get();
    setupIntent = confirmSetupIntent(setupIntent).get();
    return setupIntent;
  }

  private CompletableFuture<SetupIntent> createSetupIntent() {
    SetupIntentFuture f = new SetupIntentFuture();
    SetupIntentParameters parameters = new SetupIntentParameters.Builder().build();
    Terminal.getInstance().createSetupIntent(parameters, f);
    return f;
  }

  private CompletableFuture<SetupIntent> retrieveSetupIntent(@NotNull String clientSecret) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance().retrieveSetupIntent(clientSecret, f);
    return f;
  }

  private CompletableFuture<SetupIntent> collectSetupPaymentMethod(
      @NotNull SetupIntent setupIntent) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance()
        .collectSetupIntentPaymentMethod(setupIntent, /*customerConsentCollected */ true, f);
    return f;
  }

  private CompletableFuture<SetupIntent> confirmSetupIntent(@NotNull SetupIntent setupIntent) {
    SetupIntentFuture f = new SetupIntentFuture();
    Terminal.getInstance().confirmSetupIntent(setupIntent, f);
    return f;
  }

  // endregion Save Card

  public Refund refund(@NotNull String chargeId, @NotNull String currency, long amount) throws ExecutionException, InterruptedException {
    VoidFuture collectRefundFuture = new VoidFuture();
    CompletableFuture<Refund> processRefundFuture = new CompletableFuture<>();
    RefundParameters parameters = new RefundParameters.Builder(chargeId, amount, currency).build();
    Terminal.getInstance().collectRefundPaymentMethod(parameters, collectRefundFuture);
    collectRefundFuture.get();
    Terminal.getInstance().confirmRefund(new RefundCallback() {
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
}
