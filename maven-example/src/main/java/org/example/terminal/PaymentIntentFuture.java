package org.example.terminal;

import com.stripe.stripeterminal.external.callable.PaymentIntentCallback;
import com.stripe.stripeterminal.external.models.PaymentIntent;
import com.stripe.stripeterminal.external.models.TerminalException;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

/**
 * Implements {@link PaymentIntentCallback} with a {@link CompletableFuture},
 * such that the Future completes on {@link PaymentIntentCallback#onSuccess(PaymentIntent)}
 * and fails exceptionally on {@link PaymentIntentCallback#onFailure(TerminalException)}.
 */
public class PaymentIntentFuture extends CompletableFuture<PaymentIntent>
    implements PaymentIntentCallback {
  @Override
  public void onFailure(@NotNull TerminalException e) {
    completeExceptionally(e);
  }

  @Override
  public void onSuccess(@NotNull PaymentIntent paymentIntent) {
    complete(paymentIntent);
  }
}
