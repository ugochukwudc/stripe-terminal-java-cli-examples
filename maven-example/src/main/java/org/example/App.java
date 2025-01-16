package org.example;

import com.stripe.stripeterminal.external.models.Reader;
import com.stripe.stripeterminal.external.models.TerminalException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.*;
import java.util.prefs.BackingStoreException;
import org.example.network.ApiClient;
import org.example.terminal.StripeTerminal;
import org.jetbrains.annotations.Nullable;
import sun.misc.Signal;
import sun.misc.SignalHandler;

public class App {
  private static final String MENU = """
        1 - Take Payment Client Side Create
        2 - Take Payment Server Side Create
        3 - Save Card Client Side Create
        4 - Save Card Server Side Create
        5 - Display Cart
        6 - Clear Reader Display
        7 - Refund
        8 - Print Offline Status
        9 - Reboot Reader
        10 - Install Updates
        11 - Disconnect Reader
        12 - Exit
""";
  private static final String DISCOVERY_MENU = """
        Select Option for discover or enter the serial number of the reader you want to connect to:
        1 - Discover Internet Readers
        2 - Discover Internet Simulators
        3 - Discover Usb Readers
""";

  public static void main(String[] args)
      throws BackingStoreException, ExecutionException, InterruptedException, SocketException, TimeoutException, TerminalException {
    ApiClient apiClient = new ApiClient();
    System.out.println("Hello world!");
    Scanner sc = new Scanner(System.in);
    Runtime.getRuntime()
        .addShutdownHook(new Thread(() -> System.out.println("Shutting down VM...")));

    // Set up application with secret key
    System.out.println("Initializing...");
    apiClient.setUp(sc);
    // Initialize the Stripe Terminal
    StripeTerminal terminal = new StripeTerminal();

    // Register a signal handler to intercept ctrl+c for cancelling ongoing operations
    // like updates, reconnect, collects
    SignalHandler handler =
            signal -> {
              System.out.println("Signal received: " + signal);
              if (!signal.getName().equals("INT") || !terminal.cancelOnGoingOperation()) {
                // Use the default signal handler
                SignalHandler.SIG_DFL.handle(signal);
              }
            };

    Signal.handle(new Signal("INT"), handler);
    CompletableFuture<Reader> connectedReaderFuture = new CompletableFuture<>();
    System.out.print(DISCOVERY_MENU);
    String line = sc.nextLine().strip();
      switch (line) {
        case "1", "2" -> {
          List<Reader> readerList = terminal.discoverInternetReaders(line.equals("2")).get();
          Reader selectedReader = selectReader(readerList);
          System.out.printf("Connecting to reader: %s \n", selectedReader);
          connectedReaderFuture.complete(terminal.connectInternetReader(Objects.requireNonNull(selectedReader)).get());
        }
        case "3" -> {
        System.out.printf("Discovering readers on %s \n", Thread.currentThread().getName());
        terminal.discoverUsbReaders(
            readers -> {
              if (!readers.isEmpty()) {
                CompletableFuture.runAsync(
                    () -> {
                      Reader selectedReader = selectReader(readers);
                      System.out.printf(
                          "Connecting to reader: %s on %s thread\n",
                          selectedReader, Thread.currentThread().getName());
                      try {
                        Reader connectedReader =
                            terminal.connectUsbReader(Objects.requireNonNull(selectedReader));
                        connectedReaderFuture.complete(connectedReader);
                      } catch (Exception e) {
                        connectedReaderFuture.completeExceptionally(e);
                      }
                    });
              }
            });
        }
        default -> connectedReaderFuture.complete(terminal.findReaderBySerialNumber(line));
      }

      // Get the connected reader, timeout after 60 seconds
    Reader connectedReader = connectedReaderFuture.orTimeout(60, TimeUnit.SECONDS).join();

    int selection;
    do {
      selection = getMenuItem(sc);
      switch (selection) {
        case 1 -> terminal.takePaymentClientSideCreate(apiClient.getCurrency());
        case 2 -> terminal.takePaymentServerSideCreate(getClientSecret(sc));
        case 3 -> terminal.saveCardClientSideCreate();
        case 4 -> terminal.saveCardServerSideCreate(getClientSecret(sc));
        case 5 -> terminal.displayCart(apiClient.getCurrency());
        case 6 -> terminal.clearReaderDisplay();
        case 7 -> {
          System.out.println("Enter chargeId for payment you want to refund: ");
          String chargeId = sc.nextLine();
          System.out.println("Enter amount to refund: ");
          long amount = sc.nextLong();
          terminal.refund(chargeId, apiClient.getCurrency(), amount);
        }
        case 8 -> terminal.printOfflineStatus();
        case 9 -> terminal.rebootReader();
        case 10 -> terminal.installUpdates();
        default -> {
          System.out.println("Disconnecting reader");
          terminal.disconnectReader();
          System.out.println("Successfully disconnected");
        }
      }
    } while (selection >= 1 && selection <= 10);
    sc.close();
  }

  private static @Nullable Reader selectReader(List<Reader> readers) {
    System.out.printf("Found %d readers: \n", readers.size());
    if (readers.isEmpty()) return null; // bail early, no readers found
    for (int i = 0; i < readers.size(); i++) {
      Reader reader = readers.get(i);
      System.out.printf(
          "%1$d - %2$s (%3$s) status: %4$s, reachable: %5$s \n",
          i,
          reader.getLabel(),
          reader.getSerialNumber(),
          reader.getNetworkStatus(),
          isReachable(reader));
    }
    if (readers.size() == 1) {
      return readers.get(0); // return the only reader we found, auto-select
    } else {
      // prompt user to select a reader
      Scanner sc = new Scanner(System.in);
      System.out.printf("Select a reader: [%1d-%2d]:", 0, readers.size() - 1);
      int selectedReader = sc.nextInt();
      return readers.get(selectedReader);
    }
  }

  private static int getMenuItem(Scanner sc) {
    System.out.println("select an option:");
    System.out.println(MENU);
    int selection = sc.nextInt();
    sc.nextLine(); // consume new line
    return selection;
  }

  private static String getClientSecret(Scanner sc) {
    System.out.print("Enter client secret: ");
    return sc.nextLine();
  }

  private static String getChargeId(Scanner sc) {
    System.out.print("Enter charge id: ");
    return sc.nextLine();
  }

  /**
   * Checks if the reader is reachable on the local area network by pinging the reader.
   *
   * @param reader - represents a Stripe Terminal Reader.
   * @return a boolean flag, which is `true` when the ip address is reachable and `false` otherwise.
   */
  private static boolean isReachable(Reader reader) {
    if (reader.getIpAddress() == null) {
      return false;
    }
    try {
      String ip = Objects.requireNonNull(reader.getIpAddress());
      return InetAddress.getByAddress(AppUtils.parseIpAddress(ip)).isReachable(100);
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  private static void checkNetwork() {
    boolean hasNetwork = callSafely(App::hasNetwork);
    boolean isStripeReachable =
        callSafely(
            () -> InetAddress.getByName("abc.com").isReachable(1000));

    System.out.printf("hasNetwork: %1b, isReachable %2b\n", hasNetwork, isStripeReachable);
  }

  private static boolean hasNetwork() throws Exception {
    Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
    final boolean[] retValue = {false};
    networkInterfaces
            .asIterator()
            .forEachRemaining(networkInterface -> {
              boolean isUp =callSafely(networkInterface::isUp);
              boolean isLoopback = callSafely(networkInterface::isLoopback);
              if (isUp && !isLoopback) {
                retValue[0] = true;
              }
              System.out.printf("Checking network %1s, isUp= %2b, isLoopback = %3b, isVirtual = %4b, name = %5s\n", networkInterface, isUp, isLoopback, networkInterface.isVirtual(), networkInterface.getDisplayName());
            });
    while ((networkInterfaces = NetworkInterface.getNetworkInterfaces()).hasMoreElements()) {
      NetworkInterface networkInterface = networkInterfaces.nextElement();
      System.out.printf("Checking network %1s, isUp= %2b, isLoopback = %3b, isVirtual = %4b, name = %5s\n", networkInterface, networkInterface.isUp(), networkInterface.isLoopback(), networkInterface.isVirtual(), networkInterface.getDisplayName());
      if (networkInterface.isUp() && !networkInterface.isLoopback()) {
        retValue[0] = true;
      }
    }

    return retValue[0];
  }

  private static void displayNetworkState() throws SocketException {
    Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
    for (NetworkInterface netint : Collections.list(nets))
      displayInterfaceInformation(netint);
  }

  static void displayInterfaceInformation(NetworkInterface netint) throws SocketException {
    System.out.printf("Display name: %s\n", netint.getDisplayName());
    System.out.printf("Name: %s\n", netint.getName());
    Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();

    for (InetAddress inetAddress : Collections.list(inetAddresses)) {
      System.out.printf("InetAddress: %s\n", inetAddress);
    }

    System.out.printf("Up? %s\n", netint.isUp());
    System.out.printf("Loopback? %s\n", netint.isLoopback());
    System.out.printf("PointToPoint? %s\n", netint.isPointToPoint());
    System.out.printf("Supports multicast? %s\n", netint.supportsMulticast());
    System.out.printf("Virtual? %s\n", netint.isVirtual());
    System.out.printf("Hardware address: %s\n",
            Arrays.toString(netint.getHardwareAddress()));
    System.out.printf("MTU: %s\n", netint.getMTU());
    System.out.print("\n");
  }

  private static boolean callSafely(Callable<Boolean> callable) {
    boolean ret;
    try {
      ret = callable.call();
    } catch (Exception e) {
      System.err.println(e);
      ret = false;
    }
    return ret;
  }
}
