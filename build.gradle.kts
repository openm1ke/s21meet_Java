import io.spring.gradle.dependencymanagement.dsl.DependencyManagementExtension
import org.gradle.api.tasks.testing.Test
import java.io.File
import com.github.spotbugs.snom.Confidence
import com.github.spotbugs.snom.Effort
import com.github.spotbugs.snom.SpotBugsExtension
import com.github.spotbugs.snom.SpotBugsTask

plugins {
    java
    id("jacoco")
    id("jacoco-report-aggregation")
    id("checkstyle")
    id("pmd")
    id("com.github.spotbugs") version "6.5.1" apply false
    id("com.diffplug.spotless") version "6.25.0" apply false
    id("org.sonarqube") version "7.1.0.6387"
    id("org.springframework.boot") version "3.5.10" apply false
    id("io.spring.dependency-management") version "1.1.7" apply false
}
fun sonarConfig(key: String): String? {
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }

    val localEnv = File(rootDir, "env/local/sonar.env")
    if (!localEnv.exists()) return null

    return localEnv.useLines { lines ->
        lines.asSequence()
            .map(String::trim)
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val idx = line.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val name = line.substring(0, idx).trim()
                if (name != key) return@mapNotNull null
                line.substring(idx + 1).trim().removeSurrounding("\"").removeSurrounding("'")
            }
            .firstOrNull()
    }
}


allprojects {
    group = "ru.izpz"
    version = (findProperty("appVersion") as String?) ?: "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")
    apply(plugin = "jacoco")
    apply(plugin = "checkstyle")
    apply(plugin = "pmd")
    apply(plugin = "com.github.spotbugs")
    apply(plugin = "com.diffplug.spotless")
    
    val mockitoVersion: String by project
    val mockitoAgent by configurations.creating {
        isTransitive = false
    }

    val springCloudVersion: String by project
    val springBootVersion: String by project
    val jacksonVersion: String by project
    val spotbugsAnnotationsVersion: String by project
    configure<DependencyManagementExtension> {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:$springBootVersion") {
                bomProperty("jackson-bom.version", jacksonVersion)
            }
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:$springCloudVersion")
            mavenBom("io.github.resilience4j:resilience4j-bom:${property("resilience4jVersion")}")
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(21)
        }
    }

    tasks.withType<SpotBugsTask>().configureEach {
        excludeFilter.set(rootProject.file("config/spotbugs/exclude.xml"))
        @Suppress("DEPRECATION")
        excludeFilter = rootProject.file("config/spotbugs/exclude.xml")
        reports.create("html") {
            required = true
        }
    }

    tasks.withType<JavaCompile> {
        options.annotationProcessorPath = configurations.annotationProcessor.get()
    }

    configurations.all {
        // Common logging exclusion
        exclude(group = "org.slf4j", module = "slf4j-simple")
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        compileOnly(
            "com.github.spotbugs:spotbugs-annotations:$spotbugsAnnotationsVersion")

        annotationProcessor("org.projectlombok:lombok")
        add("mockitoAgent", "org.mockito:mockito-core:$mockitoVersion")
        // SpotBugs runtime: add logger backend and Lombok annotation classes
        add("spotbugs", "com.github.spotbugs:spotbugs:4.9.8")
        add("spotbugs", "org.slf4j:slf4j-nop:2.0.17")
        add("spotbugs", "org.projectlombok:lombok:1.18.30")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
    }

    val testTask = tasks.named<Test>("test")
    tasks.register<Test>("unitTest") {
        group = "verification"
        description = "Runs fast unit tests (excludes *IntegrationTest)"
        dependsOn("testClasses")
        testClassesDirs = testTask.get().testClassesDirs
        classpath = testTask.get().classpath
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
        exclude("**/*IntegrationTest.class")
        shouldRunAfter("test")
    }

    tasks.register<Test>("integrationTest") {
        group = "verification"
        description = "Runs integration tests (*IntegrationTest)"
        dependsOn("testClasses")
        testClassesDirs = testTask.get().testClassesDirs
        classpath = testTask.get().classpath
        useJUnitPlatform()
        jvmArgs("-javaagent:${mockitoAgent.singleFile.absolutePath}")
        include("**/*IntegrationTest.class")
        shouldRunAfter("unitTest")
    }

    configure<CheckstyleExtension> {
        toolVersion = "10.26.1"
        val checkstyleJar = configurations.checkstyle.get().files.first { it.name.startsWith("checkstyle-") }
        config = resources.text.fromArchiveEntry(checkstyleJar, "google_checks.xml")
        configProperties =
            mapOf(
                "checkstyle.suppressions.file" to
                    rootProject.file("config/checkstyle/suppressions.xml").absolutePath,
                "org.checkstyle.google.suppressionfilter.config" to
                    rootProject.file("config/checkstyle/suppressions.xml").absolutePath)
        isIgnoreFailures = false
    }

    configure<PmdExtension> {
        toolVersion = "7.13.0"
        ruleSetFiles = files()
        ruleSets = listOf("category/java/errorprone.xml")
        isConsoleOutput = true
        isIgnoreFailures = false
    }

    configure<SpotBugsExtension> {
        ignoreFailures = false
        effort = Effort.MAX
        reportLevel = Confidence.MEDIUM
        showProgress = true
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    tasks.withType<Pmd>().configureEach {
        exclude("**/build/generated/**")
        exclude("**/generated/**")
        exclude { it.file.absolutePath.contains("${File.separator}build${File.separator}generated${File.separator}") }
        if (name == "pmdTest") {
            ruleSetFiles = files(rootProject.file("config/pmd/pmd-test-ruleset.xml"))
            ruleSets = listOf()
        }
        ignoreFailures = false
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        java {
            target("src/main/java/**/*.java", "src/test/java/**/*.java")
            googleJavaFormat("1.19.2")
            trimTrailingWhitespace()
            endWithNewline()
        }
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

            classDirectories.setFrom(
                files(classDirectories.files.map { directory ->
                    fileTree(directory).apply {
                        exclude("**/dto/**")
                        exclude("**/model/**")
                        exclude("**/entity/**")
                        exclude("**/generated/**")
                        exclude("**/mapper/**")
                        exclude("**/*MapperImpl.class")
                        exclude("**/*MapperImpl$*.class")
                        exclude("**/openapi/**")
                        exclude("**/common/**")
                    }
                })
            )
        }
    }
}

sonarqube {
    properties {
        val sonarBinaryDirs = listOf(
            "s21auth/build/classes/java/main",
            "s21edu/build/classes/java/main",
            "s21bot/build/classes/java/main",
            "s21rocket/build/classes/java/main",
            "s21web/build/classes/java/main",
            "common/build/classes/java/main"
        ).joinToString(",")

        sonarConfig("SONAR_PROJECT_KEY")?.let { property("sonar.projectKey", it) }
        sonarConfig("SONAR_ORGANIZATION")?.let { property("sonar.organization", it) }
        sonarConfig("SONAR_HOST_URL")?.let { property("sonar.host.url", it) }
        sonarConfig("SONAR_TOKEN")?.let { property("sonar.token", it) }
        property("sonar.java.binaries", sonarBinaryDirs)
        property(
            "sonar.exclusions",
            "**/generated/**,**/openapi/**,**/common/**,**/dto/**,**/model/**,**/entity/**," +
                "**/src/main/resources/static/js/**"
        )
        property(
            "sonar.coverage.exclusions",
            "**/generated/**,**/openapi/**,**/common/**,**/dto/**,**/model/**,**/entity/**,**/mapper/**," +
                "**/src/main/resources/static/js/**," +
                "s21auth/src/main/java/ru/izpz/auth/config/H2TcpServerConfig.java," +
                "s21edu/src/main/java/ru/izpz/edu/config/H2TcpServerConfig.java"
        )
        property("sonar.gradle.scanAll", "false")
    }
}

tasks.register("buildAllJars") {
    group = "build"
    description = "Собирает bootJar для всех модулей"

    dependsOn(
        ":s21auth:bootJar",
        ":s21edu:bootJar",
        ":s21bot:bootJar",
        ":s21web:bootJar"
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

val orderedSubprojectTests = subprojects
    .map { it.tasks.named<Test>("test") }

tasks.register("runAllTests") {
    group = "verification"
    description = "Запускает все тесты во всех subprojects"

    dependsOn(orderedSubprojectTests)
}

tasks.register("runStaticAnalysisOnTests") {
    group = "verification"
    description = "Запускает Checkstyle и PMD для тестовых исходников во всех subprojects"
    dependsOn(
        subprojects.map { it.path + ":checkstyleTest" } +
            subprojects.map { it.path + ":pmdTest" }
    )
}

tasks.register("runAllUnitTests") {
    group = "verification"
    description = "Runs unit tests in all subprojects"
    dependsOn(subprojects.map { it.path + ":unitTest" })
}

tasks.register("runAllIntegrationTests") {
    group = "verification"
    description = "Runs integration tests in all subprojects"
    dependsOn(subprojects.map { it.path + ":integrationTest" })
}
