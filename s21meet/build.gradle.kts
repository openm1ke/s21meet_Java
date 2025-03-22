plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.11.0"
}


val springBootVersion: String by project
val junitJupiterVersion: String by project
val testcontainersVersion: String by project
val resilience4jVersion: String by project
val mockitoVersion: String by project
val mapstructVersion: String by project
val openApiVersion: String by project
val gsonVersion: String by project
val jacksonDatabind: String by project
val squareupOkhttpVersion: String by project
val gsonfireVersion: String by project
val springRetryVersion: String by project
val apacheCommonsVersion: String by project
val springDotEnvVersion: String by project
val wireMockVersion: String by project
val jettyServerVersion: String by project

dependencies {

    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openApiVersion")

    implementation("org.telegram:telegrambots-meta:8.2.0")
    implementation("org.telegram:telegrambots-longpolling:8.2.0")
    implementation("org.telegram:telegrambots-client:8.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // https://mvnrepository.com/artifact/me.paulschwarz/spring-dotenv
    implementation("me.paulschwarz:spring-dotenv:4.0.0")

    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testImplementation("org.apache.commons:commons-compress:$apacheCommonsVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
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


springBoot {
    mainClass.set("ru.school21.meet.Application")
}
