plugins {
    java
    id("jacoco")
    id("jacoco-report-aggregation")
    id("org.sonarqube") version "6.2.0.5505"
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}


allprojects {
    group = "ru.izpz"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<JavaCompile> {
        options.annotationProcessorPath = configurations.annotationProcessor.get()
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        configurations.all {
            exclude(group = "org.slf4j", module = "slf4j-simple")
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withId("jacoco") {
        jacoco {
            toolVersion = "0.8.12"
        }

        tasks.withType<Test> {
            // После выполнения тестов запускаем генерацию отчёта
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)
                html.required.set(true)
                csv.required.set(false)
            }
        }
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", env.SONAR_PROJECT_KEY.value)
        property("sonar.organization", env.SONAR_ORGANIZATION.value)
        property("sonar.host.url", env.SONAR_HOST_URL.value)
        property("sonar.token", env.SONAR_TOKEN.value)
        property("sonar.coverage.jacoco.xmlReportPaths", "s21auth/build/reports/jacoco/test/jacocoTestReport.xml," +
                "s21edu/build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.scm.disabled", "true")
        property("sonar.exclusions", "**/generated/**, **/openapi/**")
        property("sonar.coverage.exclusions", "**/generated/**, **/openapi/**")
    }
}

tasks.register("buildAllJars") {
    group = "build"
    description = "Собирает bootJar для всех модулей"

    dependsOn(
        ":s21auth:bootJar",
        ":s21edu:bootJar",
        ":s21bot:bootJar"
    )
}

tasks.register("runAllTestsWithCoverage") {
    group = "verification"
    description = "Запускает все тесты и собирает отчёты Jacoco во всех модулях"

    dependsOn(subprojects.flatMap {
        listOf(
            it.path + ":test",
            it.path + ":jacocoTestReport"
        )
    })
}

tasks.register("runAllTests") {
    group = "verification"
    description = "Запускает все тесты во всех subprojects"

    dependsOn(subprojects.map { it.path + ":test" })
}
