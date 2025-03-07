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

    configurations {
        compileOnly {
            extendsFrom(configurations.annotationProcessor.get())
        }
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
        property("sonar.projectKey", "openm1ke_s21meet_Java")
        property("sonar.organization", "openm1ke")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.token", "REMOVED_SONAR_TOKEN")
        property("sonar.coverage.jacoco.xmlReportPaths", "build/test-results/test/TEST-*.xml")
        property("sonar.scm.disabled", "true")
    }
}