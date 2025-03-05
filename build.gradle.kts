plugins {
    java
    id("jacoco")
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
}

tasks.jacocoTestReport {
    reports {
        xml.required = true
    }
}

sonarqube {
    properties {
        property("sonar.projectKey", env.SONAR_PROJECT_KEY)
        property("sonar.organization", env.SONAR_ORGANIZATION)
        property("sonar.host.url", env.SONAR_HOST_URL)
        property("sonar.token", env.SONAR_TOKEN)
    }
}