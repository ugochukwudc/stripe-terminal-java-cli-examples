package org.example.terminal;

import com.stripe.stripeterminal.external.callable.OfflineListener;
import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A simple {@link TerminalListener} implementation that just logs events to the console.
 */
public class Listener implements TerminalListener, OfflineListener {
  private ConnectionStatus connectionStatus = null;
  private PaymentStatus paymentStatus = null;

  private OfflineStatus offlineStatus = null;

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

  @Override
  public void onOfflineStatusChange(@NotNull OfflineStatus offlineStatus) {
    System.out.printf("onOfflineStatusChanged from %1$s -> %2$s.\n", this.offlineStatus, offlineStatus);
    this.offlineStatus = offlineStatus;
  }
}
