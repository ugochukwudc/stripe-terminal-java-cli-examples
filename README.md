# Stripe Terminal Java CLI Examples

A collection of example command-line applications built with the **Stripe Terminal Java SDK**.

This repository demonstrates how to integrate Stripe Terminal into a Java application for in-person payments.

-----

## ⚠️ Please Note

This repository contains two examples. The `maven-example` is the complete, up-to-date version. The `gradle-example` is an incomplete demonstration and should not be used. All instructions below apply to the **`maven-example`**.

-----

## Getting Started 🚀

### Prerequisites

  * **Java Development Kit (JDK)**: Version 8 or higher. You can check your version by running `java -version`.
  * **Apache Maven**: You must have Maven installed on your system to build this project. You can verify it's installed by running `mvn -v`.
      * If you don't have Maven, please follow the official installation instructions available at [maven.apache.org/install.html](https://maven.apache.org/install.html).

### 1\. Clone the Repository

Open your terminal and clone the repository to your local machine.

```bash
git clone https://github.com/ugochukwudc/stripe-terminal-java-cli-examples.git
```

### 2\. Build the Application

Navigate to the `maven-example` directory and use the `mvn` command to build the project. This command will compile the source code, run tests, and package the application into a single executable JAR file.

```bash
# Navigate to the correct directory
cd stripe-terminal-java-cli-examples/maven-example/

# Build the project using Maven
mvn clean package
```

A successful build will end with a `[INFO] BUILD SUCCESS` message. The final application file, `stripe-terminal-cli-2.0-SNAPSHOT.jar`, will be created inside the `target` directory.


### 3\. Run the Application

Execute the JAR file from the command line, passing your token as an argument.

```bash
java -jar target/StripeTerminalCLI-2.0-SNAPSHOT.jar
```

The application will start and ask for your secret key. Make sure the secret key you provide is for an account that have at least one terminal reader resgitered into it. 
