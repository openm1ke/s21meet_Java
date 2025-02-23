plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.11.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-logging")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0") // Добавляем OkHttp
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0") // Для логирования запросов
    implementation("com.google.code.gson:gson:2.8.9")
    // https://mvnrepository.com/artifact/io.gsonfire/gson-fire
    implementation("io.gsonfire:gson-fire:1.9.0")
    // https://mvnrepository.com/artifact/org.openapitools/jackson-databind-nullable
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")



    implementation("org.postgresql:postgresql")
    //runtimeOnly("org.postgresql:postgresql")

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api
    implementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.12.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.12.0")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/swagger.json") // Путь к вашему API JSON
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath) // Место для сгенерированных классов
    packageName.set("ru.school21.edu.generated")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",  // Используем java.time (LocalDate, LocalDateTime)
            "useJakartaEe" to "true",  // Для использования Jakarta Persistence
            "generateApis" to "true", // генерируем API-клиенты
            "generateModels" to "true", // Генерируем только модели
            "apiPackage" to "ru.school21.edu.generated.api",
            "modelPackage" to "ru.school21.edu.generated.model", // Пакет для DTO
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")", // Сущности JPA
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")\n@jakarta.persistence.Id\n@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)"
        )
    )
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

springBoot {
    mainClass.set("ru.school21.edu.Application")  // Указываем путь к главному классу
}

//sourceSets {
//    main {
//        java {
//            srcDirs("src/main/java", "$buildDir/generated/src")
//        }
//    }
//}
