package org.example;

import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.models.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import org.example.terminal.SetupIntentFuture;
import org.example.terminal.VoidFuture;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@Timeout(60)
public class StripeTerminalSaveCardTests extends StripeTerminalTests {
  Map<String, String> metaData =
      new HashMap<String, String>() {
        {
          put("foo", "bar");
          put("test", "user");
          put("id", UUID.randomUUID().toString());
        }
      };

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testSaveCard(boolean enableCustomerCancellation)
      throws ExecutionException, InterruptedException {
    // Create
    SetupIntentFuture createSetupIntentFuture = new SetupIntentFuture();
    SetupIntentParameters parameters =
        new SetupIntentParameters.Builder()
            .setDescription("test card")
            .setUsage("on_session")
            .setMetadata(metaData)
            .build();
    terminal.getTerminal().createSetupIntent(parameters, createSetupIntentFuture);
    SetupIntent createdSetupIntent = createSetupIntentFuture.get();

    // Collect
    SetupIntentFuture collectSetupIntentFuture = new SetupIntentFuture();
    SetupIntentConfiguration configuration =
        new SetupIntentConfiguration.Builder()
            .setEnableCustomerCancellation(enableCustomerCancellation)
            .build();
    Cancelable cancelable =
        terminal
            .getTerminal()
            .collectSetupIntentPaymentMethod(
                createdSetupIntent, true, configuration, collectSetupIntentFuture);
    SetupIntent collectedSetupIntent = collectSetupIntentFuture.get();
    Assertions.assertTrue(cancelable.isCompleted());

    // Confirm
    SetupIntentFuture confirmSetupIntentFuture = new SetupIntentFuture();
    terminal.getTerminal().confirmSetupIntent(collectedSetupIntent, confirmSetupIntentFuture);
    SetupIntent confirmedSetupIntent = confirmSetupIntentFuture.get();

    // Verify
    Assertions.assertEquals(confirmedSetupIntent.getMetadata(), metaData);
    Assertions.assertEquals(SetupIntentUsage.ON_SESSION, confirmedSetupIntent.getUsage());
    Assertions.assertNull(confirmedSetupIntent.getCustomerId());
    Assertions.assertEquals("test card", confirmedSetupIntent.getDescription());
    Assertions.assertNotNull(confirmedSetupIntent.getId());
    System.out.printf(
      """
      ==================================================================================================================
      Confirmed Setup Intent:
      params => %s
      Setup Intent => %s
      ==================================================================================================================
      """, parameters, confirmedSetupIntent
    );
  }

  @Test
  void testCustomerCancellation() throws ExecutionException, InterruptedException {
    // Create
    SetupIntentFuture createSetupIntentFuture = new SetupIntentFuture();
    SetupIntentParameters parameters =
        new SetupIntentParameters.Builder()
            .setDescription("test card")
            .setUsage("on_session")
            .setMetadata(metaData)
            .build();
    terminal.getTerminal().createSetupIntent(parameters, createSetupIntentFuture);
    SetupIntent createdSetupIntent = createSetupIntentFuture.get();

    // Collect
    SetupIntentConfiguration configuration =
        new SetupIntentConfiguration.Builder().setEnableCustomerCancellation(true).build();
    SetupIntentFuture collectFuture = new SetupIntentFuture();
    Cancelable cancelable =
        terminal
            .getTerminal()
            .collectSetupIntentPaymentMethod(
                createdSetupIntent, true, configuration, collectFuture);
    try {
      collectFuture.join();
      Assertions.fail("Expected customer cancellation"); // always fail if we get here
    } catch (CompletionException e) {
      Assertions.assertInstanceOf(TerminalException.class, e.getCause());
      Assertions.assertEquals(
          TerminalException.TerminalErrorCode.CANCELED,
          ((TerminalException) e.getCause()).getErrorCode());
    }
  }

  @Test
  void testProgrammaticCancellation() throws InterruptedException {
    // Create
    SetupIntentFuture createSetupIntentFuture = new SetupIntentFuture();
    SetupIntentParameters parameters =
        new SetupIntentParameters.Builder()
            .setDescription("test card")
            .setUsage("off_session")
            .setMetadata(metaData)
            .build();
    terminal.getTerminal().createSetupIntent(parameters, createSetupIntentFuture);
    SetupIntent createdSetupIntent = createSetupIntentFuture.join();

    // Collect
    SetupIntentConfiguration configuration =
        new SetupIntentConfiguration.Builder().setEnableCustomerCancellation(true).build();
    SetupIntentFuture collectFuture = new SetupIntentFuture();
    Cancelable cancelable =
        terminal
            .getTerminal()
            .collectSetupIntentPaymentMethod(
                createdSetupIntent, true, configuration, collectFuture);
    delay(500); // wait for half a second
    VoidFuture cancelCallback = new VoidFuture();
    // Cancel the collect operation
    cancelable.cancel(cancelCallback);
    cancelCallback.join();
    try {
      collectFuture.join();
      Assertions.fail("Expected programmatic cancellation"); // always fail if we get here
    } catch (CompletionException e) {
      Assertions.assertInstanceOf(TerminalException.class, e.getCause());
      Assertions.assertEquals(
          TerminalException.TerminalErrorCode.CANCELED,
          ((TerminalException) e.getCause()).getErrorCode());
    }
  }
}
