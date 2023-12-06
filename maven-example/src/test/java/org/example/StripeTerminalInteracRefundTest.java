package org.example;

import com.stripe.exception.StripeException;
import com.stripe.model.Charge;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.models.Refund;
import com.stripe.stripeterminal.external.models.RefundConfiguration;
import com.stripe.stripeterminal.external.models.RefundParameters;
import com.stripe.stripeterminal.external.models.TerminalException;
import java.util.*;
import java.util.concurrent.CompletionException;
import org.example.terminal.RefundFuture;
import org.example.terminal.VoidFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class StripeTerminalInteracRefundTest extends StripeTerminalTests {
  private static Charge refundableCharge;
  private static final Map<String, String> metaData =
      Map.of("foo", "bar", "unique_id", UUID.randomUUID().toString());

  @BeforeAll
  static void initializeRefundablePaymentIntents() throws StripeException {
    System.out.println("Calling before all in refunds tests!");
    refundableCharge = apiClient.getRefundableCharge();
  }

  @Test
  void testRefundWithChargeId() {
    VoidFuture collectFuture = new VoidFuture();
    RefundConfiguration configuration =
        new RefundConfiguration.Builder().setEnableCustomerCancellation(false).build();
    RefundParameters refundParameters =
        new RefundParameters.Builder(
                refundableCharge.getId(), 100L, refundableCharge.getCurrency())
            .setMetadata(metaData)
            .build();
    terminal
        .getTerminal()
        .collectRefundPaymentMethod(refundParameters, configuration, collectFuture);
    collectFuture.join();
    RefundFuture confirmFuture = new RefundFuture();
    terminal.getTerminal().confirmRefund(confirmFuture);
    Refund refund = confirmFuture.join();
    Assertions.assertNotNull(refund.getId());
    Assertions.assertEquals(refundParameters.getAmount(), refund.getAmount());
    Assertions.assertEquals(refundParameters.getCurrency(), refund.getCurrency());
    Assertions.assertEquals(metaData, refund.getMetadata());
  }

  @Test
  void testProgrammaticCancel() throws InterruptedException {
    RefundParameters refundParameters =
        new RefundParameters.Builder(
                refundableCharge.getId(), 100L, refundableCharge.getCurrency())
            .setMetadata(metaData)
            .build();
    VoidFuture collectFuture = new VoidFuture();
    RefundConfiguration configuration =
        new RefundConfiguration.Builder().setEnableCustomerCancellation(false).build();
    Cancelable cancelable =
        terminal
            .getTerminal()
            .collectRefundPaymentMethod(refundParameters, configuration, collectFuture);
    delay(500L); // wait for 1/2 sec
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

  @Test
  void testCustomerCancel() {
    VoidFuture collectFuture = new VoidFuture();
    RefundParameters refundParameters =
        new RefundParameters.Builder(
                refundableCharge.getId(), 100L, refundableCharge.getCurrency())
            .setMetadata(metaData)
            .build();
    RefundConfiguration configuration =
        new RefundConfiguration.Builder().setEnableCustomerCancellation(true).build();
    terminal
        .getTerminal()
        .collectRefundPaymentMethod(refundParameters, configuration, collectFuture);
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
}
