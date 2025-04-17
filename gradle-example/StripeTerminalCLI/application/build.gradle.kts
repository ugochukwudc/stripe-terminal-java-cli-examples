plugins {
    id("java")
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "org.example"
version = "2.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

application {
    mainClass.set("org.example.Main")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://d37ugbyn3rpeym.cloudfront.net/terminal/java-betas")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.stripe:stripeterminal-java:1.0.0-b13")
    implementation("com.stripe:stripe-java:+")
}

tasks.test {
    useJUnitPlatform()
}