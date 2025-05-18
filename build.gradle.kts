plugins {
    java
    id("jacoco")
    id("jacoco-report-aggregation")
    id("org.sonarqube") version "6.0.1.5171"
    id("org.springframework.boot") version "3.4.3"
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
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    tasks.withType<JavaCompile> {
        options.annotationProcessorPath = configurations.annotationProcessor.get()
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter")
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }

    plugins.withId("jacoco") {
        jacoco {
            toolVersion = "0.8.12" // укажите актуальную версию
        }

        tasks.withType<Test> {
            // После выполнения тестов запускаем генерацию отчёта
            finalizedBy(tasks.named("jacocoTestReport"))
        }

        tasks.named<JacocoReport>("jacocoTestReport") {
            reports {
                xml.required.set(true)  // XML-отчёт нужен Sonar
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
                "s21edu_api/build/reports/jacoco/test/jacocoTestReport.xml")
        property("sonar.scm.disabled", "true")
        property("sonar.exclusions", "**/generated/**, **/openapi/**")
        property("sonar.coverage.exclusions", "**/generated/**, **/openapi/**")
    }
}