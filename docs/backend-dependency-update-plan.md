# Аудит зависимостей и план обновления backend

Дата проверки: 1 октября 2026. Ограничение: Java 21 и Spring Boot 3.5.x сохраняются. Версии сверены с Maven Central metadata, официальными GitHub Releases, PyPI и документацией производителей. «Доступен релиз» означает наличие опубликованной версии; совместимость конкретного обновления должна подтверждаться проверками проекта.

## Устройство проекта

Gradle multi-module: пять Spring Boot приложений и общая библиотека.

| Модуль | Назначение |
| --- | --- |
| common | Общие DTO и OpenAPI-клиент, генерация Java через OpenAPI Generator, Jackson, HTTP-библиотеки, OpenFeign |
| s21auth | Получение и предоставление токенов внешнего API; JPA |
| s21edu | Основная предметная логика: профили, кампусы, проекты, участники, события, scheduler; JPA, PostgreSQL, Liquibase, REST/GraphQL-клиенты |
| s21bot | Telegram long polling, регистрация, кнопки и уведомления |
| s21rocket | Интеграция Rocket.Chat, WebSocket |
| s21web | Web MVC/Thymeleaf интерфейс и обращения к backend через OpenFeign |

Версии централизованы частично: properties находятся в `gradle.properties`, но версии Gradle plugins и quality tools также заданы в root/module build files. Boot plugin и Boot BOM заданы отдельно. `buildAllJars` не включает `s21rocket`, хотя CI собирает его Docker image.

## Граф знаний

Используется Graphify (`graphifyy` в PyPI), локально установлен **0.9.73**. MCP запускает `graphify-mcp graphify-out/graph.json`. Граф существует, `graphify query` работает; wiki отсутствует. Граф помогает ориентироваться, исходники необходимо проверять напрямую, особенно при текущих незакоммиченных изменениях.

Обычный цикл — AST update и локальная кластеризация через `scripts/graphify_local_update.sh`; скрипт очищает cloud LLM variables. Labels и semantic extraction запускаются отдельно через локальный Ollama: `qwen2.5-coder:7b` и производная `graphify-qwen2.5-coder:7b`. Установка Neo4j для текущего workflow не требуется.

Graphify обновлён до **0.9.73**; в этой версии есть исправление разрешения Java inherited methods и `super.method()`, полезное для этого проекта. После обновления выполнены CLI query, MCP help и штатный local update-wrapper: граф перестроен до 3163 nodes / 11727 edges / 152 communities. Перед следующими обновлениями сохранять локальную копию `graphify-out/` вне Git. Цель для Ollama остаётся **0.35.0** вместо клиента **0.34.4**: сервер во время проверки не работал, поэтому обновление Ollama ещё не выполнялось. Модель не менять вместе с CLI.

Источники: [Graphify release](https://github.com/Graphify-Labs/graphify/releases/tag/v0.9.73), [Ollama release](https://github.com/ollama/ollama/releases/tag/v0.35.0).

## Логи и метрики

Приложения используют SLF4J/Logback из Boot starters, Actuator и Micrometer Prometheus registry. После обновления фактически разрешаются Logback **1.5.38**, SLF4J **2.0.20**, Micrometer **1.15.12**. Есть собственные метрики Telegram/buttons/delivery/errors в `MetricsService` и scheduler/API/durations/campus metrics в `SchedulerMetricsService`.

Compose содержит Prometheus **3.15.0** и Grafana **13.2.3** с persistent volumes. Prometheus опрашивает пять приложений каждые 15 секунд, включая `s21web:8085`. В текущем конфиге нет infrastructure exporters и alert rules. Provisioning Grafana содержит datasource; dashboards импортируются вручную. Фактический dashboard — `monitoring/dashboards/MainDashboard s21meet-1774144549482.json`.

Отдельного log backend/collector в Compose нет: доступны консольные логи контейнеров и чтение через Docker MCP. Compose не задаёт ротацию логов; фактические daemon defaults не проверены. Нет настроенной распределённой трассировки. Рекомендуемая отдельная задача — JSON logging встроенными средствами Boot 3.5, correlation ID, ограничение объёма Docker logs; при необходимости общего поиска добавить Alloy → Loki → Grafana. Это расширение наблюдаемости, а не обязательная часть обновления библиотек.

| Компонент | Сейчас | Целевая версия / действие |
| --- | --- | --- |
| Logback | 1.5.38 | Поддерживать 1.5.x; core/classic обновлять одновременно |
| SLF4J | 2.0.20 | Поддерживать 2.0.x единообразно для API и providers |
| Micrometer | 1.15.12 | Оставить: актуальная 1.15.x для Boot 3.5; 1.17.x не ставить автоматически |
| Prometheus | 3.15.0 | Проверить migration guide, snapshot TSDB и test contour |
| Grafana | 13.2.3 | Проверить backup БД, plugins, dashboards и последовательную миграцию |
| Loki | Не используется | Опционально 3.7.8 |
| Alloy | Не используется | Опционально 1.20.1 |

Версии инфраструктуры не привязаны к major Spring Boot. Для отката Prometheus/Grafana нужны резервные копии volumes, а не только старый image tag. Новые labels/JSON поля не должны содержать пароли, токены или персональные данные; request/user IDs не использовать как metric/Loki labels с высокой кардинальностью.

Источники: [Logback 1.5 metadata](https://repo.maven.apache.org/maven2/ch/qos/logback/logback-classic/maven-metadata.xml), [SLF4J metadata](https://repo.maven.apache.org/maven2/org/slf4j/slf4j-api/maven-metadata.xml), [Micrometer metadata](https://repo.maven.apache.org/maven2/io/micrometer/micrometer-core/maven-metadata.xml), [Prometheus release](https://github.com/prometheus/prometheus/releases/tag/v3.15.0), [migration guide](https://prometheus.io/docs/prometheus/latest/migration/), [Grafana release](https://github.com/grafana/grafana/releases/tag/v13.2.3), [upgrade guide](https://grafana.com/docs/grafana/latest/upgrade-guide/), [Loki release](https://github.com/grafana/loki/releases/tag/v3.7.8), [Alloy release](https://github.com/grafana/alloy/releases/tag/v1.20.1).

## Анализ кода и форматирование

| Инструмент | Сейчас | Актуальная версия | Роль / замечание |
| --- | --- | --- | --- |
| Checkstyle | 14.3.0 | Поддерживать на JDK 21+ | Google checks из JAR, local suppressions; текущий запуск даёт 26 предупреждений text block formatting |
| PMD | 7.28.0 | Поддерживать 7.x | Error-prone rules, отдельные main/test rulesets |
| SpotBugs engine / annotations | 4.10.4 | Поддерживать 4.10.x | Bytecode analysis; синхронизировать extension, explicit tools dependency и annotations |
| SpotBugs Gradle plugin | 6.5.12 | Поддерживать 6.x | Обновлять отдельно от engine |
| Spotless Gradle plugin | 8.10.3 | Поддерживать 8.x | Форматирование Java; `spotlessCheck` проходит |
| google-java-format | 1.36.1 | 1.36.1 | Уже актуален, сохранить закреплённую версию при смене Spotless |
| JaCoCo | 0.8.15 | 0.8.15 | Уже актуален; отчёты XML/HTML, исключения DTO/model/generated |
| Sonar Gradle plugin | 7.5.0.8588 | Поддерживать 7.x | Анализ с ожиданием quality gate; 7.5 исправляет regression multi-project из 7.4 |
| actionlint | Local 1.7.12 | 1.7.12 | Workflows + ShellCheck integration; уже актуален |
| hadolint | 2.15.1 | Поддерживать 2.x | Dockerfile lint; DL3008/DL3066 остаются non-blocking warnings |
| gitleaks | Local/CI 8.30.1 | Поддерживать 8.x | CLI выровнен; текущий fast scan сообщает 7 существующих находок |

Spotless применён к subprojects, а PR workflow теперь запускает отдельный `spotlessCheck` job перед Sonar. SpotBugs имеет отдельный workflow. Следует проверить required checks в настройках GitHub: их наличие нельзя установить чтением YAML.

SpotBugs filter подавляет EI_EXPOSE_REP/EI_EXPOSE_REP2 глобально, хотя комментарий говорит о DTO. Checkstyle содержит широкие исключения. Пересмотр suppressions — отдельная задача после обновления, чтобы не смешивать появившиеся предупреждения со сменой versions.

`quality_local.sh fast` действительно запущен и завершился с exit 1: actionlint обнаружил SC2129 в `.github/workflows/build-image.yml:76` (несколько перенаправлений в один файл). Bash syntax и JSON MCP прошли; последующие Hadolint/Gitleaks стадии не выполнялись из-за `set -e`. Поиск `-name 'Dockerfile*'` также пропускает `s21edu/migrator.Dockerfile`; это нужно исправить при доработке wrapper.

Источники: [Checkstyle и JRE requirements](https://checkstyle.org/), [PMD metadata](https://repo.maven.apache.org/maven2/net/sourceforge/pmd/pmd-java/maven-metadata.xml), [SpotBugs metadata](https://repo.maven.apache.org/maven2/com/github/spotbugs/spotbugs/maven-metadata.xml), [SpotBugs plugin](https://github.com/spotbugs/spotbugs-gradle-plugin/releases/tag/6.5.12), [Spotless release](https://github.com/diffplug/spotless/releases/tag/gradle/8.10.3), [Spotless requirements](https://github.com/diffplug/spotless/blob/main/plugin-gradle/README.md), [formatter metadata](https://repo.maven.apache.org/maven2/com/google/googlejavaformat/google-java-format/maven-metadata.xml), [JaCoCo metadata](https://repo.maven.apache.org/maven2/org/jacoco/org.jacoco.core/maven-metadata.xml), [Sonar release](https://github.com/SonarSource/sonar-scanner-gradle/releases/tag/7.5.0.8588), [actionlint](https://github.com/rhysd/actionlint/releases/tag/v1.7.12), [Hadolint](https://github.com/hadolint/hadolint/releases/tag/v2.15.1), [Gitleaks](https://github.com/gitleaks/gitleaks/releases/tag/v8.30.1).

## Платформа и управление версиями

Spring Boot **3.5.16**, Spring Cloud **2025.0.3**, Spring Security **6.5.11**, Resilience4j **2.4.0**, dependency-management plugin **1.1.7** уже актуальны в выбранных ветках. Сохраняем Boot 3 / Cloud 2025.0 / Framework 6 / Security 6 / Jackson 2. Обновление до Cloud 2025.1 не входит в план: эта линия относится к Boot 4.

Gradle wrapper **8.14.5**. Глобально есть Gradle 9.8.0, но Boot 3.5 официально поддерживает Gradle 7/8: Gradle 9 в этот план не включён. Java toolchain — 21, Docker — Temurin 21, CI — Java 21; локальный Gradle launcher сейчас Corretto **23.0.2**. Для повторяемости проверки запускать на JDK 21.

Обнаруженные несогласованности:

- JUnit Jupiter/Platform теперь выровнены через `bomProperty` на **5.14.4/1.14.4**, без JUnit 6.
- `mockito-core`, `mockito-junit-jupiter` и javaagent теперь разрешаются как **5.24.0** через общий BOM property.
- Lombok processors и BOM override выровнены на **1.18.48**.
- PostgreSQL JDBC override централизован на **42.7.13** для всех модулей.
- `openFeignVersion` и `dependencyManagementPluginVersion` properties не подключены к версиям соответствующих plugin/dependency; простая смена этих properties ничего не обновит.
- `resilience4j-micrometer` 2.4.0 запрашивает Micrometer 1.16.0, но dependency management разрешает 1.15.12. Не поднимать Micrometer вслепую: проверить runtime registry и resilience metrics; это найденное расхождение, а не доказанный сбой.
- Dependency verification отключена/lenient. После стабилизации versions отдельно ввести checksums и проверку supply chain.

Источники: [Boot 3.5 requirements](https://docs.spring.io/spring-boot/3.5/system-requirements.html), [Boot BOM](https://repo.maven.apache.org/maven2/org/springframework/boot/spring-boot-dependencies/3.5.16/spring-boot-dependencies-3.5.16.pom), [Cloud compatibility](https://spring.io/projects/spring-cloud/), [Gradle releases](https://gradle.org/releases/).

## Порядок обновления

Каждый этап — самостоятельный небольшой PR с зафиксированными versions и своим результатом проверки.

1. **Исходная точка и CI.** Snapshot-коммит создан; SC2129 исправлен, migrator Dockerfile теперь попадает в Hadolint-поиск, Hadolint установлен локально на 2.15.1, Gitleaks local/CI выровнен на 8.30.1, а `spotlessCheck` добавлен отдельным PR job.
2. **Сборка и анализаторы с небольшими изменениями.** Выполнено: Gradle 8.14.5; Sonar 7.5.0.8588; PMD 7.28.0; SpotBugs plugin 6.5.12 и engine/annotations 4.10.4. Все соответствующие Gradle задачи прошли.
3. **Patch обновления runtime/test libraries.** Выполнено: Jackson BOM 2.22.3, nullable 0.2.12, Okio 3.18.2, org.json 20260814; Lombok 1.18.48, Mockito 5.24.0, SLF4J 2.0.20, Logback 1.5.38; JUnit 5.14.4/1.14.4 и PostgreSQL JDBC 42.7.13. `./gradlew test` прошёл.
4. **Форматирование и Checkstyle.** Выполнено: Spotless 8.10.3 с google-java-format 1.36.1 и Checkstyle 14.3.0 на JDK 21. `spotlessCheck` проходит; Checkstyle оставляет 26 предупреждений text block formatting в существующем коде.
5. **Граф и MCP.** Graphify 0.9.73 обновлён; query, MCP help и local update прошли. Ollama остаётся 0.34.4: Homebrew пока не предлагает 0.35.0. Playwright/GitHub/Postgres MCP обновляются отдельным этапом после фиксации образов/версий.
6. **База и миграции.** Liquibase runtime и migrator image обновлены до 4.33.0; `s21edu:test` и Compose config прошли. Validate/update на реальной test PostgreSQL и проверка checksums ждут запуска Docker/Postgres.
7. **Метрики.** Scrape s21web добавлен, docs dashboard исправлены, Prometheus/Grafana tags обновлены до 3.15.0/13.2.3. Runtime миграция, PromQL, plugins и dashboards не проверены: Docker daemon недоступен.
8. **Логи, если нужен централизованный поиск.** Сначала JSON stdout/correlation ID/ротация, затем Alloy 1.20.1 и Loki 3.7.8 с retention и Grafana datasource. Оценить объём и срок хранения. Распределённую трассировку вынести в отдельную задачу после логов и метрик.
9. **HTTP/Telegram major upgrades — отдельный завершающий этап.** OkHttp 4.12.0 → 5.5.0 и TelegramBots 9.6.0 → 10.3.0 не требуют сами по себе Boot 4, но это API/transitive migrations. Проверить Telegram client/long polling/proxy/timeouts, REST clients и совместимость Okio. Если команда хочет только patch updates, пока оставить эти две версии. Testcontainers 1.21.4 оставить; 2.0.5 требует отдельного пересмотра координат/API, в базовый этап не входит.
10. **Поддержание актуальности.** Настроить Dependabot/Renovate для Gradle, Actions и Docker с группами Boot BOM / test BOM / analyzers / observability и запретами Boot 4, Framework 7, Security 7, Jackson 3, JUnit 6. Major updates сторонних SDK подавать отдельно. Закрепить плавающие image tags/digests, добавить периодический dependency vulnerability scan; Gitleaks и Sonar не заменяют проверку CVE зависимостей.

Дополнительная инфраструктура: Caddy 2.9-alpine → кандидат 2.11.4 отдельным PR с тестом маршрутов/TLS; Temurin 21 пересобрать с актуальными security patches и закрепить digest. Xray использует `latest`: определить реально используемый image и закрепить проверенный tag/digest. Версии Docker Desktop/Compose и running image digests этим аудитом не проверены.

## Команды при выполнении плана

```bash
./gradlew --version
scripts/quality_local.sh fast
./gradlew runAllUnitTests
./gradlew :common:test :s21edu:test :s21bot:test
./gradlew spotlessCheck checkstyleMain checkstyleTest pmdMain pmdTest spotbugsMain
./gradlew check runAllTestsWithCoverage :s21auth:bootJar :s21edu:bootJar :s21bot:bootJar :s21rocket:bootJar :s21web:bootJar
```

Для анализа фактически выбранной версии:

```bash
./gradlew :s21edu:dependencyInsight --dependency jackson-databind --configuration runtimeClasspath
./gradlew :s21edu:dependencyInsight --dependency micrometer-core --configuration runtimeClasspath
./gradlew :s21bot:dependencyInsight --dependency junit-jupiter --configuration testRuntimeClasspath
./gradlew :s21bot:dependencyInsight --dependency mockito --configuration testRuntimeClasspath
```

Sonar с уже настроенными локальными credentials: `./gradlew runAllTestsWithCoverage sonar -Dsonar.qualitygate.wait=true`. После изменения поддерживаемого кода/config обязательно `scripts/graphify_local_update.sh`. Проверки PostgreSQL/Compose выполнять только в test контуре; браузерный smoke s21web — через Playwright после запуска сервиса.

В ходе выполнения плана выполнены Graphify query/update, Gradle version и dependency resolution, OpenAPI generation, `./gradlew test`, `s21edu:test`, Spotless, Checkstyle, PMD, SpotBugs, actionlint и `docker compose config --quiet`. `quality_local.sh fast` доходит до Gitleaks и сообщает 7 существующих находок; Hadolint пропущен, так как не установлен. Sonar upload, запуск сервисов и runtime миграция Prometheus/Grafana не выполнялись из-за отсутствующего Docker daemon.

## Сверка остальных явно заданных зависимостей

Ниже целевые версии в рамках Boot 3, а не максимальные major из репозитория. Источник в каждой строке — metadata производителя в Maven Central.

| Зависимость | Сейчас | Цель | Условие | Источник |
| --- | --- | --- | --- | --- |
| Jackson BOM | 2.22.3 | Поддерживать Jackson 2.22.x | Patch в используемой Jackson 2 | [metadata](https://repo.maven.apache.org/maven2/com/fasterxml/jackson/jackson-bom/maven-metadata.xml) |
| Jackson nullable | 0.2.12 | Поддерживать 0.2.x | Patch; JSON null/absent тесты | [metadata](https://repo.maven.apache.org/maven2/org/openapitools/jackson-databind-nullable/maven-metadata.xml) |
| Mockito | 5.24.0 | Поддерживать 5.x | Выравнивание core/integration/agent | [metadata](https://repo.maven.apache.org/maven2/org/mockito/mockito-core/maven-metadata.xml) |
| Lombok | 1.18.48 | Поддерживать 1.18.x | Все annotation processors | [metadata](https://repo.maven.apache.org/maven2/org/projectlombok/lombok/maven-metadata.xml) |
| Okio JVM | 3.18.2 | Поддерживать 3.x | Проверка вместе с HTTP клиентами | [metadata](https://repo.maven.apache.org/maven2/com/squareup/okio/okio-jvm/maven-metadata.xml) |
| org.json | 20260814 | Поддерживать актуальный release | s21rocket | [metadata](https://repo.maven.apache.org/maven2/org/json/json/maven-metadata.xml) |
| JUnit Jupiter | 5.14.4 / Platform 1.14.4 | Поддерживать JUnit 5 | Без JUnit 6.1.3 | [metadata](https://repo.maven.apache.org/maven2/org/junit/jupiter/junit-jupiter/maven-metadata.xml) |
| Testcontainers | 1.21.4 | 1.21.4 | Последняя 1.x; 2.0.5 отдельно | [metadata](https://repo.maven.apache.org/maven2/org/testcontainers/testcontainers/maven-metadata.xml) |
| MapStruct | 1.6.3 | 1.6.3 | 1.7 beta исключена | [metadata](https://repo.maven.apache.org/maven2/org/mapstruct/mapstruct/maven-metadata.xml) |
| Gson | 2.14.0 | 2.14.0 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/com/google/code/gson/gson/maven-metadata.xml) |
| Gson Fire | 1.9.0 | 1.9.0 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/io/gsonfire/gson-fire/maven-metadata.xml) |
| PostgreSQL JDBC | 42.7.13 | Поддерживать 42.7.x | Централизованный override | [metadata](https://repo.maven.apache.org/maven2/org/postgresql/postgresql/maven-metadata.xml) |
| Resilience4j | 2.4.0 | 2.4.0 | Проверка metrics linkage | [metadata](https://repo.maven.apache.org/maven2/io/github/resilience4j/resilience4j-bom/maven-metadata.xml) |
| Spring Retry | 2.0.13 | 2.0.13 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/org/springframework/retry/spring-retry/maven-metadata.xml) |
| Commons Compress | 1.28.0 | 1.28.0 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/org/apache/commons/commons-compress/maven-metadata.xml) |
| TelegramBots | 9.6.0 | 9.6.0 или кандидат 10.3.0 | Major отдельно | [metadata](https://repo.maven.apache.org/maven2/org/telegram/telegrambots-meta/maven-metadata.xml) |
| OkHttp / logging-interceptor | 4.12.0 | 4.12.0 или кандидат 5.5.0 | Major отдельно, обе зависимости вместе | [metadata](https://repo.maven.apache.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml) |
| Spring Security Crypto | 6.5.11 | 6.5.11 | Последняя 6.5.x | [metadata](https://repo.maven.apache.org/maven2/org/springframework/security/spring-security-crypto/maven-metadata.xml) |
| Java-WebSocket | 1.6.0 | 1.6.0 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/org/java-websocket/Java-WebSocket/maven-metadata.xml) |
| OpenAPI Generator | 7.25.0 | Поддерживать 7.x | Генерация common проверена компиляцией и тестами | [metadata](https://repo.maven.apache.org/maven2/org/openapitools/openapi-generator/maven-metadata.xml) |
| Dependency Management plugin | 1.1.7 | 1.1.7 | Уже актуален | [metadata](https://repo.maven.apache.org/maven2/io/spring/gradle/dependency-management-plugin/maven-metadata.xml) |

OpenAPI Generator обновлять после patch-библиотек, до HTTP major upgrades: генерация влияет на common и всех его потребителей. Сохранить `library=restclient`, Jackson 2 и Jakarta настройки; выполнить `./gradlew :common:openApiGenerate :common:test :s21edu:test :s21bot:test :s21web:test`.
