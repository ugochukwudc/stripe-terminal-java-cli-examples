package org.example;

import com.stripe.stripeterminal.external.models.*;
import org.example.network.ApiClient;
import org.example.terminal.StripeTerminal;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;

@Timeout(60)
public class StripeTerminalCollectInputsTests extends StripeTerminalTests {
  private static final String VERY_LONG_STRING =
      """
          This is a very long string that which we want to use for testing forms inputs string threshold to ensure that
          it throws an error for string that are longer than the set max threshold for forms. We have set a max because
          the reader has limited real estate so we need to be efficient with how we use the screen space. This surely
          exceeds the maximum, and can be reused for tests seeking to check that max. Thanks!
          """;


  @Test
  void testCollectInputsWith10Inputs() {
    List<Input> inputs =
        List.of(
            new SignatureInput.Builder("Enter your signature")
                .setDescription("We need you signature for da thing")
                .setRequired(true)
                .build(),
            new EmailInput.Builder("Customer Email")
                .setDescription("Enter your email for promotions")
                .build(),
            new PhoneInput.Builder("Enter raffle draw").build(),
            new SelectionInput.Builder("Do you agree to terms and conditions")
                .setRequired(true)
                .build(),
            new EmailInput.Builder("Another Customer Email")
                .setDescription("Enter your email for promotions")
                .build(),
            new PhoneInput.Builder("Enter raffle draw again ").build(),
            new SelectionInput.Builder("Are you sure you agree to the terms and conditions")
                .setButtons(
                    List.of(
                        new SelectionButton(
                            SelectionButtonStyle.PRIMARY, "Yes, I'm very very very sure"),
                        new SelectionButton(SelectionButtonStyle.PRIMARY, "Yes, I'm very sure"),
                        new SelectionButton(SelectionButtonStyle.SECONDARY, "No I'm not sure")))
                .setRequired(false)
                .build(),
            new NumericInput.Builder("enter your lucky numbers").build(),
            new TextInput.Builder("enter your favorite word").build(),
            new SignatureInput.Builder("Another signing form").build(),
            new SignatureInput.Builder("Last one I promise").build());

    Throwable error =
            Assertions.assertThrows(
                    TerminalException.class,
                    () -> terminal.collectInputs(
                            new CollectInputsParameters(inputs)));
    System.out.println(
            "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + error);
    System.out.println(
            "===============================================================================================");
  }

  @Test
  void testEmptyCollectInputs() {
    Throwable error =
        Assertions.assertThrows(
            TerminalException.class,
            () -> {
              terminal.collectInputs(new CollectInputsParameters(List.of()));
            });
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + error);
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testVeryLongTitleCollectInputs() {
    Throwable error =
            Assertions.assertThrows(
                    TerminalException.class,
                    () -> {
                      terminal.collectInputs(
                              new CollectInputsParameters(
                                      List.of(new SignatureInput.Builder(VERY_LONG_STRING).build())
                              )
                      );
                    });
    System.out.println(
            "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + error);
    System.out.println(
            "===============================================================================================");
  }

  @Test
  void testCollectSignature() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(
                    new SignatureInput.Builder("Please sign below")
                        .setDescription("We need your signature for a blank cheque")
                        .build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testCollectPhoneNumber() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(new PhoneInput.Builder("Enter your Phone number").build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testCollectEmail() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(new EmailInput.Builder("Enter your email address: ").build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testCollectNumberInput() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(new NumericInput.Builder("Enter your lucky numbers").build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testCollectTextInput() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(new TextInput.Builder("Enter your favorite word").build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testCollectSelectionInput() throws Throwable {
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(
            new CollectInputsParameters(
                List.of(
                    new SelectionInput.Builder("Agree to the terms and conditions")
                        .setButtons(
                            List.of(
                                new SelectionButton(
                                    SelectionButtonStyle.PRIMARY, "Yes, I'm very very very sure"),
                                new SelectionButton(
                                    SelectionButtonStyle.PRIMARY, "Yes, I'm very sure"),
                                new SelectionButton(
                                    SelectionButtonStyle.SECONDARY, "No I'm not sure")))
                        .setRequired(false)
                        .build())));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testMultipleForms() throws Throwable {
    List<Input> inputs = new ArrayList<>();
    inputs.add(
        new SignatureInput.Builder("Enter your signature")
            .setDescription("We need you signature for da thing")
            .setRequired(true)
            .build());
    inputs.add(
        new EmailInput.Builder("Customer Email")
            .setDescription("Enter your email for promotions")
            .build());
    inputs.add(new PhoneInput.Builder("Enter raffle draw").build());
    inputs.add(
        new SelectionInput.Builder("Do you agree to terms and conditions")
            .setButtons(
                List.of(
                    new SelectionButton(SelectionButtonStyle.PRIMARY, "Advance"),
                    new SelectionButton(SelectionButtonStyle.SECONDARY, "Decline")))
            .setRequired(true)
            .build());
    List<? extends CollectInputsResult> list =
        terminal.collectInputs(new CollectInputsParameters(inputs));
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + list.toString());
    System.out.println(
        "===============================================================================================");
  }

  @Test
  void testVeryLongDescription() throws Throwable {
    Throwable error =
        Assertions.assertThrows(
            TerminalException.class,
            () -> {
              terminal.collectInputs(
                  new CollectInputsParameters(
                      List.of(
                          new EmailInput.Builder("Enter your email address: ")
                              .setDescription(VERY_LONG_STRING)
                              .build())));
            });
    System.out.println(
        "===============================================================================================");
    System.out.println("Collect Inputs test complete:: " + error);
    System.out.println(
        "===============================================================================================");
  }
}
