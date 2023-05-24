package org.example.terminal;

import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.PaymentStatus;
import com.stripe.stripeterminal.external.models.Reader;
import org.jetbrains.annotations.NotNull;

/**
 * A simple {@link TerminalListener} implementation that just logs events to the console.
 */
public class Listener implements TerminalListener {
  private ConnectionStatus connectionStatus = null;
  private PaymentStatus paymentStatus = null;

  @Override
  public void onConnectionStatusChange(@NotNull ConnectionStatus status) {
    System.out.printf("onConnectionStatusChange from %1$s -> %2$s\n", connectionStatus, status);
    connectionStatus = status;
  }

  @Override
  public void onPaymentStatusChange(@NotNull PaymentStatus status) {
    System.out.printf("onPaymentStatusChange from %1$s -> %2$s\n", paymentStatus, status);
    paymentStatus = status;
  }

  @Override
  public void onUnexpectedReaderDisconnect(@NotNull Reader reader) {
    throw new RuntimeException(String.format("onUnexpectedReaderDisconnect from %1$s", reader));
  }
}
