package org.example.terminal;

import com.stripe.stripeterminal.external.callable.RefundCallback;
import com.stripe.stripeterminal.external.models.Refund;
import com.stripe.stripeterminal.external.models.TerminalException;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Implements {@link RefundCallback} with a {@link CompletableFuture},
 * such that the Future completes on {@link RefundCallback#onSuccess(Refund)}
 * and fails exceptionally on {@link RefundCallback#onFailure(TerminalException)}.
 */
public class RefundFuture extends CompletableFuture<Refund> implements RefundCallback {
    @Override
    public void onFailure(@NotNull TerminalException e) {
        completeExceptionally(e);
    }

    @Override
    public void onSuccess(@NotNull Refund refund) {
        complete(refund);
    }
}
