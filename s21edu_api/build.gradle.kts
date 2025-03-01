plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.11.0"
}

dependencies {
    implementation(project(":s21auth"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.11.0")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.1")
    implementation("com.fasterxml.jackson.core:jackson-core:2.14.1")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.14.1")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.14.1")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    // https://mvnrepository.com/artifact/com.squareup.okhttp3/logging-interceptor
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // https://mvnrepository.com/artifact/io.gsonfire/gson-fire
    implementation("io.gsonfire:gson-fire:1.8.1")

    implementation("org.postgresql:postgresql")
    implementation("com.google.code.gson:gson:2.12.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:2.2.0")
    implementation("org.springframework.retry:spring-retry:2.0.11")
    implementation("org.springframework.boot:spring-boot-starter-aop")

    // Зависимости Spring Boot для JPA и тестирования
    implementation("org.springframework.boot:spring-boot-starter")

    testImplementation("org.springframework.boot:spring-boot-starter-test")

    testImplementation("org.testcontainers:testcontainers:1.19.8")
    testImplementation("org.apache.commons:commons-compress:1.26.0")
    testImplementation("org.testcontainers:postgresql:1.19.8")
    testImplementation("org.testcontainers:junit-jupiter:1.19.8")

    testImplementation("org.junit.jupiter:junit-jupiter:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.9.3")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.9.3")
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

tasks.named("openApiGenerate") {
    doLast {
        val stubFile = file("src/main/resources/stub/JSON.java")
        val generatedFile = file("${buildDir}/generated/src/main/java/ru/school21/edu/JSON.java")
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
