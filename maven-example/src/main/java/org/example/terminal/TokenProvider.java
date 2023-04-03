package org.example.terminal;

import com.stripe.exception.StripeException;
import com.stripe.model.terminal.ConnectionToken;
import com.stripe.param.terminal.ConnectionTokenCreateParams;
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;
import org.jetbrains.annotations.NotNull;

/** Implements {@link ConnectionTokenProvider} using the ApiClient to fetch a connection token. */
public class TokenProvider implements ConnectionTokenProvider {
  @Override
  public void fetchConnectionToken(@NotNull ConnectionTokenCallback connectionTokenCallback) {
    try {
      ConnectionToken token = ConnectionToken.create(ConnectionTokenCreateParams.builder().build());
      connectionTokenCallback.onSuccess(token.getSecret());
    } catch (StripeException e) {
      connectionTokenCallback.onFailure(new ConnectionTokenException("Fetch token failed", e));
    }
  }
}
