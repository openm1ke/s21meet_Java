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

dependencies {
    implementation(project(":s21auth"))

    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.11.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    implementation("io.gsonfire:gson-fire:1.8.1")

    implementation("org.postgresql:postgresql")
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:$resilience4jVersion")
    implementation("org.springframework.retry:spring-retry:2.0.11")
    implementation("org.springframework.boot:spring-boot-starter-aop:$springBootVersion")

    // Зависимости Spring Boot для JPA и тестирования
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }

    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    // https://mvnrepository.com/artifact/org.apache.commons/commons-compress
    testImplementation("org.apache.commons:commons-compress:1.27.1")
    // https://mvnrepository.com/artifact/org.mockito/mockito-core
    testImplementation("org.mockito:mockito-core:5.14.2")


    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitJupiterVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-params:$junitJupiterVersion")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/swagger.json") // Путь к вашему API JSON
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath) // Место для сгенерированных классов
    packageName.set("ru.school21.edu")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",  // Используем java.time (LocalDate, LocalDateTime)
            "useJakartaEe" to "true",  // Для использования Jakarta Persistence
            "generateApis" to "true", // генерируем API-клиенты
            "generateModels" to "true", // Генерируем только модели
            "apiPackage" to "ru.school21.edu.api",
            "modelPackage" to "ru.school21.edu.model", // Пакет для DTO
            "jsonLibrary" to "jackson",
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")", // Сущности JPA
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")\n@jakarta.persistence.Id\n@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)"
        )
    )
}

configurations.all {
    resolutionStrategy {
        // Фиксируем версию, чтобы не подтягивалась старая версия junit-platform-commons
        force("org.junit.platform:junit-platform-commons:1.11.4")
    }
}


tasks.named("openApiGenerate") {
    doLast {
        val stubFile = file("src/main/resources/stub/JSON.java")
        val generatedFile = layout.buildDirectory.dir("generated/src/main/java/ru/school21/edu/JSON.java").get().asFile
        if (stubFile.exists()) {
            println("Перезаписываем сгенерированный JSON.java на версию-стаб из: ${stubFile.absolutePath}")
            stubFile.copyTo(generatedFile, overwrite = true)
        }
    }
}
tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

springBoot {
    mainClass.set("ru.school21.edu.Application")  // Указываем путь к главному классу
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", layout.buildDirectory.dir("generated/src/main/java").get().asFile)
        }
    }
}
