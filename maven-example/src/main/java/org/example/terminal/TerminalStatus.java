package org.example.terminal;

import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.OfflineStatus;
import com.stripe.stripeterminal.external.models.PaymentStatus;
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
     * @param predicate - Condition to satisfy in order to return from this function
     */
    @NotNull VoidFuture waitForOfflineStatus(Predicate<OfflineStatus> predicate);

    /**
     * Helper method to wait until the {@linkplain #getConnectionStatus()} and {@linkplain #getPaymentStatus()} satisfies the predicate.
     * @param predicate to satisfy to return from this function.
     */
    @NotNull VoidFuture waitFor(BiPredicate<PaymentStatus, ConnectionStatus> predicate);
}
