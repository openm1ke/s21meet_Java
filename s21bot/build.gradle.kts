plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    id("org.openapi.generator") version "7.13.0"
}

val testcontainersVersion: String by project
val mockitoVersion: String by project
val mapstructVersion: String by project
val openApiVersion: String by project
val squareupOkhttpVersion: String by project
val apacheCommonsVersion: String by project
val springDotEnvVersion: String by project
val telegramBotsVersion: String by project
val okioJvmVersion: String by project
val openFeignVersion: String by project
val springSecurityCryptoVersion: String by project
val resilience4jVersion: String by project

dependencies {
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign") {
        exclude(group = "org.springframework.security", module = "spring-security-crypto")
    }
    implementation("org.springframework.security:spring-security-crypto:$springSecurityCryptoVersion")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:${resilience4jVersion}")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:${resilience4jVersion}")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openApiVersion")
    implementation("org.telegram:telegrambots-meta:$telegramBotsVersion")
    implementation("org.telegram:telegrambots-longpolling:$telegramBotsVersion")
    implementation("org.telegram:telegrambots-client:$telegramBotsVersion")
    implementation("com.squareup.okhttp3:okhttp:$squareupOkhttpVersion")
    implementation("com.squareup.okio:okio-jvm:$okioJvmVersion")
    implementation("me.paulschwarz:springboot3-dotenv:${springDotEnvVersion}")

    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testImplementation("org.apache.commons:commons-compress:$apacheCommonsVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

tasks.test {
    useJUnitPlatform()
}

configurations.all {
    resolutionStrategy {
        // Фиксируем версию, чтобы не подтягивалась старая версия junit-platform-commons
        force("org.junit.platform:junit-platform-commons:1.11.4")
    }
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("${project.name}.jar")
    mainClass.set("ru.izpz.bot.S21BotApplication")
}

springBoot {
    mainClass.set("ru.izpz.bot.S21BotApplication")
}
