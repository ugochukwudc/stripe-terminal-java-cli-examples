package org.example;

import com.stripe.stripeterminal.external.callable.Callback;
import com.stripe.stripeterminal.external.models.TerminalException;

import java.util.concurrent.CountDownLatch;

public class FakeCallback implements Callback {
    private String id;
    private CountDownLatch latch;

    FakeCallback() {
        this.id = null;
        this.latch = new CountDownLatch(1);
    }

    FakeCallback(String id) {
        this.id = id;
        this.latch = new CountDownLatch(1);
    }

    @Override
    public void onSuccess() {
        latch.countDown();
        System.out.println("Success invoked for id: " + id);
    }

    @Override
    public void onFailure(TerminalException e) {
        latch.countDown();
        System.out.println("Failure: " + e.getMessage() + " for id: " + id);
    }

    public void await() throws InterruptedException {
        latch.await();
    }
}
