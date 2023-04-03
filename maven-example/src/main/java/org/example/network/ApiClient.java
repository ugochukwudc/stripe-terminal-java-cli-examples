package org.example.network;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

public class ApiClient {
  private static final String PREF_SECRET_KEY = "stripe-secret-key";
  private static final String DEFAULT_SECRET = "sk_";
  private final Preferences preferences;
  private Account account;

  public ApiClient(Preferences preferences) {
    this.preferences = preferences;
    account = null;
  }

  public ApiClient() {
    this(Preferences.userRoot());
  }

  public @NotNull String secretKey() {
    return preferences.get(PREF_SECRET_KEY, DEFAULT_SECRET);
  }

  /** Sets the {@link ApiClient} up for making network calls */
  public void setUp(Scanner sc) throws BackingStoreException {
    if (secretKey().equals(DEFAULT_SECRET)) {
      // User has initialized a secret key,
      // Prompt for secret key and save for future use
      System.err.println("Couldn't find a secret key for this User!");
      promptForSecretKey(sc);
    }

    // We have a secret key
    Stripe.apiKey = secretKey();
    // Attempt to fetch the Account Info
    try {
      account = Account.retrieve();
    } catch (StripeException e) {
      System.err.println("Failed to fetch account with secret key");
      preferences.remove(PREF_SECRET_KEY);
      preferences.flush();
    }
  }

  public @NotNull String getCurrency() {
    return account.getDefaultCurrency();
  }

  private void promptForSecretKey(Scanner sc) throws BackingStoreException {
    // Prompt for secret key and save for future use
    System.out.println("Please enter a secret key: ");
    String key = sc.nextLine();
    // Save the entered key
    preferences.put(PREF_SECRET_KEY, key);
    preferences.flush();
    preferences.sync();
  }
}
