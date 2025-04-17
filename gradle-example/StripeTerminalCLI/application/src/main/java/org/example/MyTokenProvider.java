package org.example;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.terminal.ConnectionToken;
import com.stripe.stripeterminal.external.callable.ConnectionTokenCallback;
import com.stripe.stripeterminal.external.callable.ConnectionTokenProvider;
import com.stripe.stripeterminal.external.models.ConnectionTokenException;

import java.util.TreeMap;

import static com.stripe.model.terminal.ConnectionToken.*;

public class MyTokenProvider implements ConnectionTokenProvider {

    MyTokenProvider() {
        Stripe.apiKey = System.getenv("STRIPE_SECRET_KEY") != null ? System.getenv("STRIPE_SECRET_KEY") : System.getProperty("STRIPE_SECRET_KEY");
    }

    @Override
    public void fetchConnectionToken(ConnectionTokenCallback connectionTokenCallback) {
        try {
            ConnectionToken token = create(new TreeMap<>());
            connectionTokenCallback.onSuccess(token.getSecret());
        } catch (StripeException e) {
            connectionTokenCallback.onFailure(new ConnectionTokenException(e.getMessage()));
        }
    }
}
