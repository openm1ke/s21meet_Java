import org.springframework.boot.gradle.tasks.bundling.BootJar
import org.gradle.jvm.tasks.Jar

plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

val testcontainersVersion: String by project
val mockitoVersion: String by project
val mapstructVersion: String by project
val springRetryVersion: String by project
val apacheCommonsVersion: String by project
val postgresqlVersion: String by project
val squareupOkhttpVersion: String by project
val springDotEnvVersion: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign") {
        exclude(group = "org.springframework.security", module = "spring-security-crypto")
    }
    implementation("me.paulschwarz:springboot3-dotenv:${springDotEnvVersion}")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.liquibase:liquibase-core")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.retry:spring-retry:$springRetryVersion")
    implementation("com.squareup.okhttp3:okhttp:${squareupOkhttpVersion}")
    implementation("com.squareup.okhttp3:logging-interceptor:${squareupOkhttpVersion}")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-ratelimiter")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.apache.commons:commons-compress:$apacheCommonsVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("com.h2database:h2")
}

tasks.test {
    useJUnitPlatform()
}

springBoot {
    mainClass.set("ru.izpz.edu.S21EduApplication")
}

tasks.named<BootJar>("bootJar") {
    archiveFileName.set("app.jar")
}

tasks.named<Jar>("jar") {
    enabled = false
}
