package org.example;

import com.stripe.stripeterminal.Terminal;
import com.stripe.stripeterminal.appinfo.ApplicationInformation;
import com.stripe.stripeterminal.external.callable.Cancelable;
import com.stripe.stripeterminal.external.models.DiscoveryConfiguration;
import com.stripe.stripeterminal.log.LogLevel;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello world!, is initialized: " + Terminal.isInitialized());
        File directory = Paths.get(System.getProperty("user.home"), "Desktop", ".example-cli").toFile();
        if (!directory.exists()) {
            directory.mkdirs();
        }
        Listener listener = new Listener();
        ApplicationInformation info = new ApplicationInformation(
                "com.example.app",
                "1.0.0",
                directory
        );
        Terminal.initTerminal(
                new MyTokenProvider(),
                listener,
                info,
                LogLevel.NONE,
                listener
        );
        System.out.println("Terminal initialized: " + Terminal.isInitialized());
        DiscoveryConfiguration configuration;
        if (System.getProperty("testInternet") != null) {
            configuration = new DiscoveryConfiguration.InternetDiscoveryConfiguration();
        } else {
            configuration = new DiscoveryConfiguration.UsbDiscoveryConfiguration(10, false);
        }
        Cancelable c = Terminal.getInstance().discoverReaders(
                configuration,
                listener,
                new FakeCallback("discoverReaders")
        );
        Thread.sleep(5_000L);
        c.cancel(new FakeCallback("cancelDiscovery"));
        System.out.println("Discover readers canceled, done executing.");
        Thread t = getThread("Thread-Dumper");
        t.start();
    }

    /**
     * This method is used to create a thread that dumps the state of all threads in the current
     * thread group.
     *
     * @param threadName - name of the thread to be created.
     * @return a Thread object that dumps the state of all threads in the current thread group.
     */
    private static Thread getThread(String threadName) {
        Thread t = new Thread(threadName) {
            @Override
            public void run() {
                System.out.println("Dumping thread state =======");
                while(Thread.currentThread().isAlive()) {
                    Thread[] threads = new Thread[Thread.activeCount()];
                    Thread.currentThread().getThreadGroup().enumerate(threads);
                    Arrays.stream(threads)
                            .filter(t -> !t.isDaemon() && t.isAlive())
                            .forEach(t -> System.out.println(t.getName() + " " + t.getState() + " " ));
                    System.out.println("Dumped thread state =======\n\n\n\n");
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        };
        t.setDaemon(true);
        return t;
    }
}