plugins {
    id("java")
}

group = "org.example"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenCentral()
        google()
        maven(url = "https://d37ugbyn3rpeym.cloudfront.net/terminal/java-betas")
    }
}


tasks.test {
    useJUnitPlatform()
}