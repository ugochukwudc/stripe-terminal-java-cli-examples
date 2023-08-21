package org.example.network;

import com.google.gson.JsonSyntaxException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import org.jetbrains.annotations.NotNull;

import java.util.Scanner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import static com.stripe.model.StripeObject.PRETTY_PRINT_GSON;

public class ApiClient {
  private static final String PREF_SECRET_KEY = "stripe-secret-key";
  private static final String ACCOUNT_KEY = "stripe-account";
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
    // Retrieve saved key & account; and ask if it is okay to proceed as is
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
      account = retrieveAccount();
      System.out.println("Got account = " + account);
    } catch (Exception e) {
      System.err.println("Failed to fetch account");
      e.printStackTrace();
    }
    boolean hasAccount = account != null;
    System.out.print((hasAccount ? "Continue" : "Retry") + " (Y/N): ");
    Setup option;
    if (sc.nextLine().equalsIgnoreCase("Y")) {
      if (hasAccount) {
        option = Setup.COMPLETE;
      } else {
        option = Setup.RETRY;
      }
    } else {
      option = Setup.RETRY_WITH_NEW_ACCOUNT;
    }

    switch (option) {
      case RETRY_WITH_NEW_ACCOUNT -> {
        preferences.remove(PREF_SECRET_KEY);
        preferences.sync();
        preferences.flush();
        setUp(sc);
      }
      case RETRY -> setUp(sc);
      case COMPLETE -> {
        return;
      }
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

  private Account retrieveAccount() throws BackingStoreException, IllegalStateException {
    // Attempt to retrieve the account from the Stripe Api safely
    Account acc;
    try {
      acc = Account.retrieve();
      // Serialize the account to json and save to Shared preferences
      preferences.put(ACCOUNT_KEY, acc.toJson());
    } catch (StripeException exception) {
      // fallback to the saved account when retrieve fails
      System.out.println("Failed to fetch account from Stripe, falling back to saved account");
      String accJsonStr = preferences.get(ACCOUNT_KEY, "");
      if (accJsonStr.isEmpty()) throw new IllegalStateException("No saved account!", exception);
      try {
      acc = PRETTY_PRINT_GSON.fromJson(accJsonStr, Account.class);
      } catch (JsonSyntaxException e){
        System.err.println("Could not parse the account json string: " + accJsonStr);
        // remove the corrupted string
        preferences.remove(ACCOUNT_KEY);
        // rethrow the error
        throw new IllegalStateException("No saved account!", e);
      }
    } finally {
      preferences.flush();
      preferences.sync();
    }

    return acc;
  }

  private void setUpAccount(Scanner sc) throws BackingStoreException {
    // Retrieve saved key & account; and ask if it is okay to proceed as is
    if (secretKey().equals(DEFAULT_SECRET)) {
      // User has not initialized a secret key,
      // Prompt for secret key and save for future use
      System.err.println("Couldn't find a secret key for this User!");
      promptForSecretKey(sc);
    }

    // We have a secret key
    Stripe.apiKey = secretKey();
    // Attempt to fetch the Account Info
    try {
      account = retrieveAccount();
    } catch (Exception e) {
      System.err.println("Failed to fetch account with secret key");
      preferences.remove(PREF_SECRET_KEY);
      preferences.flush();
      throw new RuntimeException(e);
    }
  }

  enum Setup {
    COMPLETE,
    RETRY,
    RETRY_WITH_NEW_ACCOUNT;
  }
}
