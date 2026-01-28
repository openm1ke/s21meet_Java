plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Apply plugins from parent project
apply(plugin = "jacoco")

val springDotEnvVersion: String by project

group = "ru.izpz"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")

    implementation("org.java-websocket:Java-WebSocket:1.6.0")
    implementation("org.json:json:20250517")
    implementation("org.slf4j:slf4j-api:2.0.17")

    implementation("me.paulschwarz:springboot3-dotenv:${springDotEnvVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}