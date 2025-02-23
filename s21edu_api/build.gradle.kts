plugins {
    java
    id("org.springframework.boot") version "3.4.3"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.11.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.openapitools:openapi-generator-gradle-plugin:7.11.0")

    runtimeOnly("org.postgresql:postgresql")
}

openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$rootDir/src/main/resources/api.yaml")
    outputDir.set("$buildDir/generated")
    packageName.set("com.example.generated")
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}
