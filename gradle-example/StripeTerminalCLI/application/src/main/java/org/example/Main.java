package org.example;

import com.stripe.stripeterminal.Terminal;

public class Main {
    public static void main(String[] args) {
        System.out.println("Hello world!, is initialized: " + Terminal.isInitialized());
    }
}