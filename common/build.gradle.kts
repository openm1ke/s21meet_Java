plugins {
    id("java-library")
    id("org.openapi.generator") version "7.14.0"
}

val openApiVersion: String by project
val jacksonDatabind: String by project
val squareupOkhttpVersion: String by project
val okioJvmVersion: String by project
val gsonVersion: String by project
val gsonfireVersion: String by project
val jacksonDatabindNullable: String by project
val springSecurityCryptoVersion: String by project

group = "ru.izpz"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabind")
    implementation("org.openapitools:jackson-databind-nullable:$jacksonDatabindNullable")
    implementation("com.squareup.okhttp3:okhttp:$squareupOkhttpVersion")
    implementation("com.squareup.okhttp3:logging-interceptor:$squareupOkhttpVersion")
    implementation("com.squareup.okio:okio-jvm:$okioJvmVersion")
    implementation("io.gsonfire:gson-fire:$gsonfireVersion")
    implementation("com.google.code.gson:gson:$gsonVersion")
    // https://mvnrepository.com/artifact/jakarta.annotation/jakarta.annotation-api
    implementation("jakarta.annotation:jakarta.annotation-api:3.0.0")

    implementation("org.springframework.cloud:spring-cloud-starter-openfeign") {
        exclude(group = "org.springframework.security", module = "spring-security-crypto")
    }
    implementation("org.springframework.security:spring-security-crypto:$springSecurityCryptoVersion")

    // https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.0")

    implementation("jakarta.validation:jakarta.validation-api:3.1.1")
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}


openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/openapi/swagger.json") // Путь к вашему API JSON
    //outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath) // Место для сгенерированных классов
    outputDir.set(layout.buildDirectory.dir("generated").get().asFile.absolutePath)
    packageName.set("ru.izpz.dto")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",  // Используем java.time (LocalDate, LocalDateTime)
            "useJakartaEe" to "true",  // Для использования Jakarta Persistence
            "generateApis" to "true", // генерируем API-клиенты
            "generateModels" to "true", // Генерируем только модели
            "apiPackage" to "ru.izpz.dto.api",
            "modelPackage" to "ru.izpz.dto.model", // Пакет для DTO
            "jsonLibrary" to "jackson",
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")", // Сущности JPA
            //"additionalModelTypeAnnotations" to "@jakarta.persistence.Entity\n@jakarta.persistence.Table(name=\"${'$'}{classname}\")\n@jakarta.persistence.Id\n@jakarta.persistence.GeneratedValue(strategy = jakarta.persistence.GenerationType.IDENTITY)"
        )
    )
}


tasks.named("openApiGenerate") {
//    doLast {
//        val stubFile = file("src/main/resources/JSON.java")
//        val generatedFile = layout.buildDirectory.dir("generated/src/main/java/ru/izpz/dto/JSON.java").get().asFile
//        if (stubFile.exists()) {
//            println("Перезаписываем сгенерированный JSON.java на версию-стаб из: ${stubFile.absolutePath}")
//            stubFile.copyTo(generatedFile, overwrite = true)
//        }
//    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

sourceSets {
    main {
        java {
            srcDirs("src/main/java", layout.buildDirectory.dir("generated/src/main/java").get().asFile)
        }
    }
}
