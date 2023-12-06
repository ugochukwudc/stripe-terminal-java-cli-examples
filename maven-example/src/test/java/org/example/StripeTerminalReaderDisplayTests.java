package org.example;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.external.models.*;
import java.util.ArrayList;
import java.util.List;
import org.example.terminal.VoidFuture;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

@Timeout(60)
public class StripeTerminalReaderDisplayTests extends StripeTerminalTests {

  @MethodSource("testParameters")
  @ParameterizedTest
  void test(@NotNull Cart cart) throws InterruptedException {
    terminal.displayCart(cart);
    delay(5_000L); // delay for 5 seconds on each test so tester can inspect the reader display
  }

  @AfterEach
  void clearReaderDisplay() {
    terminal.clearReaderDisplay();
  }

  static @NotNull List<Cart> testParameters() {
    List<Cart> carts = new ArrayList<>();
    List<CartLineItem> lineItems = new ArrayList<>();
    int accTotal = 0;
    for (int i = 1; i < 20; i++) {
      lineItems.add(new CartLineItem.Builder("line item " + i, i, i * 1_000).build());
      int total = i * i * 1000;
      accTotal += total;
      Cart.Builder cartBuilder = new Cart.Builder(apiClient.getCurrency(), accTotal / 5, accTotal);
      cartBuilder.setLineItems(lineItems);
      carts.add(cartBuilder.build());
    }
    return carts;
  }
}
