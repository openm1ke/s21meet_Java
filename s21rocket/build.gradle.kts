plugins {
    id("org.springframework.boot") version "3.5.10"
    id("io.spring.dependency-management") version "1.1.7"
    java
}

val springDotEnvVersion: String by project

group = "ru.izpz.rocket"
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

    implementation("me.paulschwarz:spring-dotenv:${springDotEnvVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}