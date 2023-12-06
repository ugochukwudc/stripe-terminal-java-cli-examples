package org.example;

import com.stripe.stripeterminal.external.models.ConnectionStatus;
import com.stripe.stripeterminal.external.models.PaymentStatus;
import com.stripe.stripeterminal.external.models.Reader;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.prefs.BackingStoreException;
import org.example.network.ApiClient;
import org.example.terminal.StripeTerminal;
import org.example.terminal.VoidFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

public class StripeTerminalTests {
  protected static StripeTerminal terminal;
  protected static ApiClient apiClient;
  private static final String TEST_READER_LABEL = "WPE3";

  @BeforeAll
  static void initializeStripeTerminal() throws BackingStoreException {
    System.out.println("Calling before all in StripeTerminalTests");
    apiClient = new ApiClient();
    Scanner sc = new Scanner("Y"); // Use Y so we can auto advance on setup.
    apiClient.setUp(sc);
    terminal = new StripeTerminal();
    sc.close();
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @BeforeEach
  void connectToTestReader()
      throws NoSuchElementException, ExecutionException, InterruptedException {
    List<Reader> readerList = terminal.discoverReaders(false).get();
    int failureCount = 0;
    do {
      System.out.println("connecting to reader: " + TEST_READER_LABEL);
      try {
        terminal
            .connectReader(
                readerList.stream()
                    .filter(reader -> Objects.equals(reader.getLabel(), TEST_READER_LABEL))
                    .findFirst()
                    .get())
            .join();
        failureCount = -1; // on success
      } catch (Exception e) {
        e.printStackTrace();
        failureCount++;
        // Apply exponential back off before next retry
        delay(failureCount * failureCount * 500L);
      }
    } while (failureCount > 0);
    System.out.println("Out of the while connecting loop, waiting for connected");
    terminal
        .waitFor(((p, c) -> p == PaymentStatus.READY && c == ConnectionStatus.CONNECTED))
        .orTimeout(5, TimeUnit.SECONDS)
        .join();
    System.out.println("Done waiting for connected");
  }

  @AfterEach
  void disconnectFromTestReader() throws Throwable {
    System.out.println("Disconnecting from " + TEST_READER_LABEL);
    terminal.disconnectReader();
    terminal
        .waitFor((p, c) -> c == ConnectionStatus.NOT_CONNECTED)
        .orTimeout(2, TimeUnit.SECONDS)
        .join();
    System.out.println("Disconnected from " + TEST_READER_LABEL);
  }

  protected void delay(final long timeInMillis) throws InterruptedException {
//    final VoidFuture f = new VoidFuture();
    Thread t =
        new Thread() {
          @Override
          public void run() {
            super.run();
            try {
              sleep(timeInMillis);
            } catch (InterruptedException e) {
              throw new RuntimeException(e);
            }
//            f.complete(null);
          }
        };
    t.setDaemon(true);
    t.start();
//    f.join();
    t.join();
  }
}
