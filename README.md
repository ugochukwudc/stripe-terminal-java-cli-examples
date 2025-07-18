# Stripe Terminal Java CLI Examples

A collection of example command-line applications built with the **Stripe Terminal Java SDK**.

This repository demonstrates how to integrate Stripe Terminal into a Java application for in-person payments.

-----

## ⚠️ Please Note

This repository contains two examples. The `maven-example` is the complete, up-to-date version. The `gradle-example` is an incomplete demonstration and should not be used. All instructions below apply to the **`maven-example`**.

-----

## Getting Started 🚀

### Prerequisites

  * **Java Development Kit (JDK)**: Version 8 or higher.
  * **No Maven Installation Needed\!** This project uses the Maven Wrapper (`mvnw`), which automatically downloads the correct Maven version for you.

### 1\. Clone the Repository

Open your terminal and clone this repository to your local machine.

```bash
git clone https://github.com/ugochukwudc/stripe-terminal-java-cli-examples.git
```

### 2\. Build the Application

Navigate into the correct project directory and use the Maven Wrapper to build the application. This command will compile the code, run tests, and package it into an executable JAR file.

```bash
# Navigate to the maven example directory
cd stripe-terminal-java-cli-examples/maven-example/StripeTerminalCLI/

# Build the project using the wrapper
# On macOS or Linux:
./mvnw clean package

# On Windows:
mvnw.cmd clean package
```

The first time you run this, it will download all the necessary dependencies. A successful build will end with a `[INFO] BUILD SUCCESS` message. The final application file, `StripeTerminalCLI-1.0-SNAPSHOT.jar`, will be located in the `target` directory.

### 3\. Run the Application

Execute the JAR file from the command line, passing your token as an argument.

```bash
java -jar target/StripeTerminalCLI-2.0-SNAPSHOT.jar
```

The application will start and ask for your secret key. Make sure the secret key you provide is for an account that have at least one terminal reader resgitered into it. 
