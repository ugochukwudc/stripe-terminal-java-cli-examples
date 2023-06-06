package org.example;

import com.stripe.stripeterminal.external.models.Reader;
import org.example.network.ApiClient;
import org.example.terminal.StripeTerminal;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;
import java.util.prefs.BackingStoreException;

public class App {
  private static final String MENU =
      "1 - Collect Payment(Client side) \n2 - Collect Payment(Server side) \n3 - Save Card(Client side) \n4 - Save Card(Server side) \n5 - Display Cart \n6 - Clear Display \n7 - Refund \n8 - Disconnect Reader";

    public static void main(String[] args) throws BackingStoreException, ExecutionException, InterruptedException {
        ApiClient apiClient = new ApiClient();
        System.out.println("Hello world!");
        Scanner sc = new Scanner(System.in);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> System.out.println("Shutting down VM...")));

        // Set up application with secret key
        apiClient.setUp(sc);
        // Initialize the Stripe Terminal
        StripeTerminal terminal = new StripeTerminal();
        System.out.println("Use simulated readers: (Y/N): ");
        boolean simulated = sc.nextLine().equalsIgnoreCase("Y");
        List<Reader> readerList = terminal.discoverReaders(simulated).get();

        Reader reader = selectReader(readerList);
        System.out.printf("Connecting to reader: %s \n", reader);
        terminal.connectReader(Objects.requireNonNull(reader)).get();

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
                default -> {
                    System.out.println("Disconnecting reader");
                    terminal.disconnectReader();
                    System.out.println("Successfully disconnected");
                }
            }
        } while (selection >= 1 && selection <= 7);
        sc.close();
    }

    private static @Nullable Reader selectReader(List<Reader> readers){
        System.out.printf("Found %d readers: \n", readers.size());
        if (readers.size() == 0) return null; // bail early, no readers found
        for (int i = 0; i < readers.size(); i++) {
            Reader reader = readers.get(i);
            System.out.printf("%1$d - %2$s (%3$s) status: %4$s, reachable: %5$s \n", i, reader.getLabel(), reader.getSerialNumber(), reader.getNetworkStatus(), isReachable(reader));
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
        try {
            String ip = Objects.requireNonNull(reader.getIpAddress());
            return InetAddress.getByAddress(AppUtils.parseIpAddress(ip)).isReachable(100);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

}