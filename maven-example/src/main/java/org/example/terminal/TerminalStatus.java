package org.example.terminal;

import com.stripe.stripeterminal.external.models.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiPredicate;
import java.util.function.Predicate;

public interface TerminalStatus {
    @Nullable ConnectionStatus getConnectionStatus();

    @Nullable PaymentStatus getPaymentStatus();

    @Nullable OfflineStatus getOfflineStatus();

  /**
   * Waits for the {@linkplain #getOfflineStatus()} to satisfy the given predicate.
   * @param predicate - tests the condition to satisfy to complete the returned future from this function.
   * @return a {@link VoidFuture} that completes when the {@param predicate#test} is true.
   */
  @NotNull
  VoidFuture waitForOfflineStatus(Predicate<OfflineStatus> predicate);

    /**
     * Helper method to wait until the {@linkplain #getConnectionStatus()} and {@linkplain #getPaymentStatus()} satisfies the predicate.
     * @param predicate - tests the condition to satisfy to complete the returned future from this function.
     * @return a {@link VoidFuture} that completes when the {@param predicate#test} is true.
     */
    @NotNull VoidFuture waitFor(BiPredicate<PaymentStatus, ConnectionStatus> predicate);

    /**
     * Helper method to wait for an Offline payment forwarding event.
     * @param predicate - tests the condition to satisfy to complete the returned future from this function.
     * @return a {@link VoidFuture} that completes when the {@param predicate#test} is true.
     */
    @NotNull VoidFuture waitForForwarding(BiPredicate<PaymentIntent, TerminalException> predicate);

}
