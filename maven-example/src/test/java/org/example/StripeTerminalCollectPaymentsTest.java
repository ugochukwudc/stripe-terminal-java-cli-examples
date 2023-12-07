package org.example;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.models.*;
import java.util.*;
import java.util.concurrent.CompletionException;

import org.example.terminal.PaymentIntentFuture;
import org.example.terminal.VoidFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@Timeout(60)
public class StripeTerminalCollectPaymentsTest extends StripeTerminalTests {
  Map<String, String> metaData =
      Map.of(
          "store-payment-id", UUID.randomUUID().toString(),
          "key", "ugo-offline",
          "foo", "bar");

  @ParameterizedTest
  @EnumSource(OfflineBehavior.class)
  void testHappyPathOnline(@NotNull OfflineBehavior offlineBehavior) {
    List<PaymentMethodType> paymentMethodTypes =
        new ArrayList<>() {
          {
            add(PaymentMethodType.CARD_PRESENT);
            add(PaymentMethodType.INTERAC_PRESENT);
          }
        };
    PaymentIntentParameters parameters =
        new PaymentIntentParameters.Builder(
                1000_00L, apiClient.getCurrency(), CaptureMethod.Automatic, paymentMethodTypes)
            // set metadata to identify the payment  when it is forwarded
            .setMetadata(metaData)
            .build();
    CreateConfiguration configuration = new CreateConfiguration(offlineBehavior);
    PaymentIntent createdPI = terminal.createPayment(parameters, configuration).join();
    PaymentIntentFuture collectFuture = new PaymentIntentFuture();
    CollectConfiguration config = new CollectConfiguration.Builder().build();
    Cancelable cancelable =
        Terminal.getInstance().collectPaymentMethod(createdPI, config, collectFuture);
    PaymentIntent collectedPI = collectFuture.join();
    PaymentIntentFuture confirmFuture = new PaymentIntentFuture();
    Terminal.getInstance().confirmPaymentIntent(collectedPI, confirmFuture);
    PaymentIntent confirmedPI = confirmFuture.join();
    Assertions.assertEquals(PaymentIntentStatus.SUCCEEDED, confirmedPI.getStatus());
    Assertions.assertEquals(metaData, confirmedPI.getMetadata());
    if (offlineBehavior == OfflineBehavior.FORCE_OFFLINE) {
      Assertions.assertTrue(
          Objects.requireNonNull(confirmedPI.getOfflineDetails()).getRequiresUpload());
    } else {
      Assertions.assertFalse(confirmedPI.getCharges().isEmpty());
    }
    System.out.printf(
            """
            ========================================================================================================================
            Payments tests
            offline behaviour => %s
            confirmed Payment Intent => %s
            offline Details => %s
            charge => %s
            ========================================================================================================================
            """, offlineBehavior, confirmedPI, confirmedPI.getOfflineDetails(), confirmedPI.getCharges());
  }

  @Test
  void testCustomerCancellation() {
    List<PaymentMethodType> paymentMethodTypes =
        new ArrayList<>() {
          {
            add(PaymentMethodType.CARD_PRESENT);
            add(PaymentMethodType.INTERAC_PRESENT);
          }
        };
    PaymentIntentParameters parameters =
        new PaymentIntentParameters.Builder(
                1000_00L, apiClient.getCurrency(), CaptureMethod.Automatic, paymentMethodTypes)
            // set metadata to identify the payment  when it is forwarded
            .setMetadata(metaData)
            .build();
    CreateConfiguration configuration = new CreateConfiguration(OfflineBehavior.PREFER_ONLINE);
    PaymentIntent createdPI = terminal.createPayment(parameters, configuration).join();
    CollectConfiguration config =
        new CollectConfiguration.Builder().setEnableCustomerCancellation(true).build();
    PaymentIntentFuture collectFuture = new PaymentIntentFuture();
    Cancelable cancelable =
        Terminal.getInstance().collectPaymentMethod(createdPI, config, collectFuture);
    try {
      collectFuture.join();
      Assertions.fail("Expected customer cancellation, so execution shouldn't reach here");
    } catch (CompletionException e) {
      Assertions.assertInstanceOf(TerminalException.class, e.getCause());
      Assertions.assertEquals(
          TerminalException.TerminalErrorCode.CANCELED,
          ((TerminalException) e.getCause()).getErrorCode());
    }
  }

  @Test
  void testProgrammaticCancellation() throws InterruptedException {
    System.out.println("Started programmatic cancel tests");
    List<PaymentMethodType> paymentMethodTypes =
        new ArrayList<>() {
          {
            add(PaymentMethodType.CARD_PRESENT);
            add(PaymentMethodType.INTERAC_PRESENT);
          }
        };
    PaymentIntentParameters parameters =
        new PaymentIntentParameters.Builder(
                1000_00L, apiClient.getCurrency(), CaptureMethod.Automatic, paymentMethodTypes)
            // set metadata to identify the payment  when it is forwarded
            .setMetadata(metaData)
            .build();
    CreateConfiguration configuration = new CreateConfiguration(OfflineBehavior.PREFER_ONLINE);
    PaymentIntent createdPI = terminal.createPayment(parameters, configuration).join();
    CollectConfiguration config =
        new CollectConfiguration.Builder().setEnableCustomerCancellation(true).build();
    PaymentIntentFuture collectFuture = new PaymentIntentFuture();
    Cancelable cancelable =
        Terminal.getInstance().collectPaymentMethod(createdPI, config, collectFuture);
    delay(1000L);
    VoidFuture cancelFuture = new VoidFuture();
    cancelable.cancel(cancelFuture);
    cancelFuture.join();
    try {
      collectFuture.join();
      Assertions.fail("Expected programmatic cancellation, so execution shouldn't reach here");
    } catch (CompletionException e) {
      Assertions.assertInstanceOf(TerminalException.class, e.getCause());
      Assertions.assertEquals(
          TerminalException.TerminalErrorCode.CANCELED,
          ((TerminalException) e.getCause()).getErrorCode());
    }
  }
}
