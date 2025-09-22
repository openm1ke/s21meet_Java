plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.13.0"
}

val springBootVersion: String by project
val testcontainersVersion: String by project
val resilience4jVersion: String by project
val mockitoVersion: String by project
val mapstructVersion: String by project
val springRetryVersion: String by project
val apacheCommonsVersion: String by project
val postgresqlVersion: String by project
val openFeignVersion: String by project
val squareupOkhttpVersion: String by project

dependencies {
    implementation(project(":s21auth"))
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign:$openFeignVersion") {
        exclude(group = "org.springframework.security", module = "spring-security-crypto")
    }
    implementation("org.springframework.boot:spring-boot-starter-aop:$springBootVersion")
    implementation("org.springframework.retry:spring-retry:$springRetryVersion")
    implementation("com.squareup.okhttp3:okhttp:${squareupOkhttpVersion}")
    implementation("com.squareup.okhttp3:logging-interceptor:${squareupOkhttpVersion}")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    implementation("org.postgresql:postgresql:$postgresqlVersion")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:$resilience4jVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.apache.commons:commons-compress:$apacheCommonsVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

configurations.all {
    resolutionStrategy {
        // Фиксируем версию, чтобы не подтягивалась старая версия junit-platform-commons
        force("org.junit.platform:junit-platform-commons:1.11.4")
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.getByName<Jar>("jar") {
    enabled = true
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = true
    archiveFileName.set("${project.name}.jar")
    mainClass.set("ru.izpz.edu.S21EduApplication")
}

springBoot {
    mainClass.set("ru.izpz.edu.S21EduApplication")
}
