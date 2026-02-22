plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

// Apply plugins from parent project
apply(plugin = "jacoco")

val javaWebSocketVersion: String by project
val jsonVersion: String by project
val slf4jVersion: String by project
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
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    implementation("org.java-websocket:Java-WebSocket:${javaWebSocketVersion}")
    implementation("org.json:json:${jsonVersion}")
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")

    implementation("me.paulschwarz:springboot3-dotenv:${springDotEnvVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}