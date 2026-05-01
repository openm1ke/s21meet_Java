import org.gradle.kotlin.dsl.named
import org.springframework.boot.gradle.tasks.bundling.BootJar

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
val resilience4jVersion: String by project

group = "ru.izpz"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${resilience4jVersion}")

    implementation("org.java-websocket:Java-WebSocket:${javaWebSocketVersion}")
    implementation("org.json:json:${jsonVersion}")
    implementation("org.slf4j:slf4j-api:${slf4jVersion}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("ru.izpz.rocket.S21RocketApplication")
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar") {
    enabled = false
}
