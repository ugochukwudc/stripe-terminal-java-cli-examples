plugins {
    id("java")
    application
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven(url = "https://d37ugbyn3rpeym.cloudfront.net/terminal/java-betas")
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation("com.stripe:stripeterminal-java:1.0.0-b8")
}

tasks.test {
    useJUnitPlatform()
}