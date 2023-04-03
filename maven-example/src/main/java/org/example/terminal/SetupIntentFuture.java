package org.example.terminal;

import com.stripe.stripeterminal.external.callable.SetupIntentCallback;
import com.stripe.stripeterminal.external.models.SetupIntent;
import com.stripe.stripeterminal.external.models.TerminalException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implements {@link SetupIntentCallback} using {@link CompletableFuture}s.
 * Such that the future completes on {@link SetupIntentCallback#onSuccess(SetupIntent)}
 * and fails on {@link SetupIntentCallback#onFailure(TerminalException)}.
 */
public class SetupIntentFuture extends CompletableFuture<SetupIntent> implements SetupIntentCallback {
    @Override
    public void onFailure(@NotNull TerminalException e) {
        completeExceptionally(e);
    }

    @Override
    public void onSuccess(@NotNull SetupIntent setupIntent) {
        complete(setupIntent);

    }
}
