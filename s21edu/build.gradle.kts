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
val openApiVersion: String by project
val gsonVersion: String by project
val jacksonDatabind: String by project
val squareupOkhttpVersion: String by project
val gsonfireVersion: String by project
val springRetryVersion: String by project
val apacheCommonsVersion: String by project
val okioJvmVersion: String by project

dependencies {
    implementation(project(":s21auth"))
    implementation(project(":common"))
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-logging:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.openapitools:openapi-generator-gradle-plugin:$openApiVersion")
    implementation("org.openapitools:jackson-databind-nullable:$jacksonDatabind")
    implementation("org.springframework.retry:spring-retry:$springRetryVersion")
    implementation("org.springframework.boot:spring-boot-starter-aop:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter:$springBootVersion")
    implementation("org.mapstruct:mapstruct:$mapstructVersion")
    implementation("org.postgresql:postgresql")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:$resilience4jVersion")
    implementation("io.github.resilience4j:resilience4j-ratelimiter:$resilience4jVersion")
    implementation("io.gsonfire:gson-fire:$gsonfireVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$squareupOkhttpVersion")
    implementation("com.squareup.okio:okio-jvm:$okioJvmVersion")
    implementation("com.squareup.okhttp3:okhttp:$squareupOkhttpVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")

    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")
    testImplementation("org.springframework.boot:spring-boot-starter-test:$springBootVersion") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    testImplementation("org.apache.commons:commons-compress:$apacheCommonsVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.testcontainers:postgresql:$testcontainersVersion")
    testImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/swagger.json") // Путь к вашему API JSON
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath) // Место для сгенерированных классов
    packageName.set("ru.izpz.edu")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",  // Используем java.time (LocalDate, LocalDateTime)
            "useJakartaEe" to "true",  // Для использования Jakarta Persistence
            "generateApis" to "true", // генерируем API-клиенты
            "generateModels" to "true", // Генерируем только модели
            "apiPackage" to "ru.izpz.edu.api",
            "modelPackage" to "ru.izpz.edu.model", // Пакет для DTO
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

sourceSets {
    main {
        java {
            srcDirs("src/main/java", layout.buildDirectory.dir("generated/src/main/java").get().asFile)
        }
    }
}
