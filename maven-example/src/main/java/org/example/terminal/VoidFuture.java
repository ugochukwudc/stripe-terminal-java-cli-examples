package org.example.terminal;

import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.models.TerminalException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implements {@link Callback} using a {@link CompletableFuture}.
 * Such that the future completes on {@link Callback#onSuccess()}
 * and fails on {@link Callback#onFailure(TerminalException)}.
 */
public class VoidFuture extends CompletableFuture<Void> implements Callback {
    @Override
    public void onSuccess() {
        complete(null);
    }

    @Override
    public void onFailure(@NotNull TerminalException e) {
        completeExceptionally(e);
    }
}
