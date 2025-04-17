package org.example;

import com.stripe.stripeterminal.external.callable.DiscoveryListener;
import com.stripe.stripeterminal.external.callable.MobileReaderListener;
import com.stripe.stripeterminal.external.callable.OfflineListener;
import com.stripe.stripeterminal.external.callable.TerminalListener;
import com.stripe.stripeterminal.external.models.*;

import java.util.List;

@SuppressWarnings("NullableProblems")
public class Listener implements TerminalListener, OfflineListener, MobileReaderListener, DiscoveryListener {
    @Override
    public void onConnectionStatusChange(ConnectionStatus status) {
        System.out.println("Connection status changed: " + status);
    }

    @Override
    public void onPaymentStatusChange(PaymentStatus status) {
        System.out.println("Payment status changed: " + status);
    }

    @Override
    public void onForwardingFailure(TerminalException e) {
        System.out.println("Forwarding failure: " + e.getMessage());
    }

    @Override
    public void onOfflineStatusChange(OfflineStatus offlineStatus) {
        System.out.println("Offline status changed: " + offlineStatus);
    }

    @Override
    public void onPaymentIntentForwarded(PaymentIntent paymentIntent, TerminalException e) {
        if (e != null) {
            System.out.println("Payment intent forwarding failed: " + e.getMessage());
        } else {
            System.out.println("Payment intent forwarded successfully: " + paymentIntent);
        }
    }

    @Override
    public void onUpdateDiscoveredReaders(List<Reader> list) {
        System.out.println("Discovered readers: " + list);
    }
}
