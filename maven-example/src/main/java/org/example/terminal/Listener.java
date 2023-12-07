package org.example.terminal;

import com.stripe.stripeterminal.external.callable.OfflineListener;
import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A simple {@link TerminalListener} implementation that just logs events to the console. */
@SuppressWarnings("CallToPrintStackTrace")
public class Listener implements TerminalListener, OfflineListener, TerminalStatus {
  private ConnectionStatus connectionStatus = null;
  private PaymentStatus paymentStatus = null;

  private OfflineStatus offlineStatus = null;

  private final ArrayList<ConnectionAndPaymentStatusWaiter> connectionAndPaymentStatusWaiters = new ArrayList<>();
  private final ArrayList<OfflineStatusWaiter> offlineStatusWaiters = new ArrayList<>();
  private final ArrayList<PaymentForwardingWaiter> paymentForwardingWaiters = new ArrayList<>();

  @Nullable
  @Override
  public ConnectionStatus getConnectionStatus() {
    return connectionStatus;
  }

  @Override
  public @Nullable PaymentStatus getPaymentStatus() {
    return paymentStatus;
  }

  @Override
  public @Nullable OfflineStatus getOfflineStatus() {
    return offlineStatus;
  }

  @Override
  public void onConnectionStatusChange(@NotNull ConnectionStatus status) {
    System.out.printf("onConnectionStatusChange from %1$s -> %2$s\n", connectionStatus, status);
    connectionStatus = status;
    testPaymentAndConnectionStatusWaiters(getPaymentStatus(), status);
  }

  @Override
  public void onPaymentStatusChange(@NotNull PaymentStatus status) {
    System.out.printf("onPaymentStatusChange from %1$s -> %2$s\n", paymentStatus, status);
    paymentStatus = status;
    testPaymentAndConnectionStatusWaiters(status, getConnectionStatus());
  }

  @Override
  public void onUnexpectedReaderDisconnect(@NotNull Reader reader) {
    throw new RuntimeException(String.format("onUnexpectedReaderDisconnect from %1$s", reader));
  }

  @Override
  public void onOfflineStatusChange(@NotNull OfflineStatus offlineStatus) {
    System.out.printf(
        "onOfflineStatusChanged from %1$s -> %2$s.\n", this.offlineStatus, offlineStatus);
    this.offlineStatus = offlineStatus;
    testOfflineStatusWaiters(offlineStatus);
  }

  @Override
  public void onForwardingFailure(@NotNull TerminalException e) {
    testPaymentForwardingWaiters(null, e);
    System.err.println("Encountered error while forwarding: " + e.getErrorCode());
    e.printStackTrace();
  }

  @Override
  public void onPaymentIntentForwarded(@NotNull PaymentIntent paymentIntent, @Nullable TerminalException e) {
    testPaymentForwardingWaiters(paymentIntent, e);
    if (e != null) {
      System.err.printf(
      """
      =========================================================================================================================
      Encountered error while forwarding
      Payment Intent : %s
      Offline Details: %s
      Error code: %s
      =========================================================================================================================
      """, paymentIntent, paymentIntent.getOfflineDetails(), e.getErrorCode());
      e.printStackTrace();
    } else {
      System.out.printf(
      """
      ==========================================================================================================================
      Successful Forward!
      Payment Intent: %s
      Offline Details: %s
      ==========================================================================================================================
      """, paymentIntent, paymentIntent.getOfflineDetails());
    }
  }

  @Override
  public @NotNull VoidFuture waitForOfflineStatus(Predicate<OfflineStatus> predicate) {
    OfflineStatusWaiter waiter = new OfflineStatusWaiter(predicate);
    offlineStatusWaiters.add(waiter);
    testOfflineStatusWaiters(getOfflineStatus());
    return waiter.future;
  }

  @Override
  public @NotNull VoidFuture waitFor(BiPredicate<PaymentStatus, ConnectionStatus> predicate) {
    ConnectionAndPaymentStatusWaiter waiter = new ConnectionAndPaymentStatusWaiter(predicate);
    connectionAndPaymentStatusWaiters.add(waiter);
    testPaymentAndConnectionStatusWaiters(getPaymentStatus(), getConnectionStatus());
    return waiter.future;
  }

  @Override
  public @NotNull VoidFuture waitForForwarding(BiPredicate<PaymentIntent, TerminalException> predicate) {
    PaymentForwardingWaiter waiter = new PaymentForwardingWaiter(predicate);
    paymentForwardingWaiters.add(waiter);
    return waiter.future;
  }

  private void testOfflineStatusWaiters(@Nullable OfflineStatus status) {
    Iterator<OfflineStatusWaiter> iterator = offlineStatusWaiters.iterator();
    while (iterator.hasNext()) {
      OfflineStatusWaiter waiter = iterator.next();
      if (waiter.offlineStatusPredicate.test(status)) {
        waiter.future.complete(null);
        iterator.remove();
      }
    }
  }

  private void testPaymentAndConnectionStatusWaiters(@Nullable PaymentStatus paymentStatus, @Nullable ConnectionStatus connectionStatus) {
    Iterator<ConnectionAndPaymentStatusWaiter> iterator = connectionAndPaymentStatusWaiters.iterator();
    while (iterator.hasNext()) {
      System.out.printf("testing connection status waiters with payment status = %s, connection status = %s \n", paymentStatus, connectionStatus);
      ConnectionAndPaymentStatusWaiter waiter = iterator.next();
      if (waiter.paymentStatusConnectionStatusBiPredicate.test(paymentStatus, connectionStatus)) {
        waiter.future.complete(null);
        iterator.remove();
      }
    }
  }

  private void testPaymentForwardingWaiters(@Nullable PaymentIntent paymentIntent, @Nullable TerminalException error) {
    Iterator<PaymentForwardingWaiter> iterator = paymentForwardingWaiters.iterator();
    while (iterator.hasNext()) {
      PaymentForwardingWaiter waiter = iterator.next();
      if (waiter.paymentForwardingPredicate.test(paymentIntent, error)) {
        waiter.future.complete(null);
        iterator.remove();
      }
    }
  }

  static class StatusWaiter {
    final VoidFuture future = new VoidFuture();
  }

  static class OfflineStatusWaiter extends StatusWaiter {
    final Predicate<OfflineStatus> offlineStatusPredicate;

    OfflineStatusWaiter(Predicate<OfflineStatus> predicate) {
      super();
      offlineStatusPredicate = predicate;
    }
  }

  static class ConnectionAndPaymentStatusWaiter extends StatusWaiter {
    final BiPredicate<PaymentStatus, ConnectionStatus> paymentStatusConnectionStatusBiPredicate;

    ConnectionAndPaymentStatusWaiter(BiPredicate<PaymentStatus, ConnectionStatus> biPredicate) {
      super();
      paymentStatusConnectionStatusBiPredicate = biPredicate;
    }
  }

  static class PaymentForwardingWaiter extends StatusWaiter {
    final BiPredicate<PaymentIntent, TerminalException> paymentForwardingPredicate;

    PaymentForwardingWaiter(BiPredicate<PaymentIntent, TerminalException>biPredicate) {
      super();
      paymentForwardingPredicate = biPredicate;
    }
  }
}
