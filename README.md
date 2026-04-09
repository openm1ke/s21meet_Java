# s21meet_Java

`s21meet_Java` — многомодульный backend-проект для Telegram-бота Школы 21 (Сбер), который помогает студентам находить пиров, управлять контактами и получать уведомления о событиях и присутствии друзей в кампусе.

Проект спроектирован как набор сервисов с разделением ответственности: отдельные модули для Telegram-бота, работы с API кампуса, токенов авторизации и интеграции с Rocket.Chat, плюс инфраструктура мониторинга и деплоя.

## Что умеет система

- Поиск и отображение пиров в кампусе.
- Добавление пользователей в друзья.
- Уведомления о приходе друзей в кампус (по подпискам в боте).
- Показ событий кампуса.
- Показ списка проектов, над которыми работает пользователь.
- Подтверждение регистрации/проверки пользователя через Rocket.Chat.

## Архитектура

Репозиторий состоит из нескольких Gradle-модулей:

- `s21bot` — Telegram-бот и бизнес-логика пользовательских сценариев.
- `s21edu` — сервис работы с API кампуса, данными профилей/участников, планировщиками и метриками.
- `s21auth` — сервис получения/хранения токенов для внешних запросов.
- `s21rocket` — интеграция с Rocket.Chat (QR/сообщения/верификация пользователя).
- `common` — общие DTO и вспомогательные артефакты.

Инфраструктура:

- `PostgreSQL` для хранения доменных данных.
- `Liquibase` для миграций схемы БД.
- `Prometheus + Grafana` для мониторинга.
- `Docker Compose` для локального/серверного запуска набора сервисов.

## Технологии

- `Java 21`
- `Spring Boot 3`
- `Spring Data JPA`
- `Liquibase`
- `PostgreSQL`
- `Gradle` (multi-module)
- `JUnit 5`, `Mockito`, `Testcontainers`
- `JaCoCo` (coverage)
- `SonarQube` (анализ качества на `master`)
- `GitHub Actions` (CI/CD)
- `Docker`, `Docker Compose`

## Структура репозитория

```text
s21meet_Java/
├── common/
├── s21auth/
├── s21bot/
├── s21edu/
├── s21rocket/
├── env/
│   ├── test/*.env.example
│   └── prod/*.env.example
├── monitoring/
├── scripts/
├── docker-compose.yml
└── dev.sh
```

## Быстрый старт (локально)

### 1. Требования

- `JDK 21`
- `Docker` + `Docker Compose`

### 2. Клонирование

```bash
git clone <repo-url>
cd s21meet_Java
chmod +x gradlew dev.sh
```

### 3. Подготовка окружения

Примеры env-файлов уже есть в репозитории:

- `env/test/*.env.example`
- `env/prod/*.env.example`

Для локального запуска через `dev.sh` файлы из `*.example` будут созданы автоматически при первом старте.

### 4. Запуск

Полный локальный запуск приложения:

```bash
./dev.sh
```

Полезные варианты:

```bash
./dev.sh infra                # только инфраструктура
./dev.sh s21edu s21bot        # запуск выбранных сервисов
./dev.sh s21bot --proxy ssh   # bot + SOCKS5 через SSH-туннель
./dev.sh --full               # полный clean/recreate сценарий
./dev.sh --down               # остановка окружения
```

### 5. Ключевые параметры `s21edu` (projects scheduler и внешние лимиты)

Основные параметры лежат в:

- `env/test/s21edu.env` (локально)
- `env/prod/s21edu.env.example` (шаблон для прода)

Параметры шедулера проектов:

- `PROJECTS_SCHEDULER_ENABLED` — включает/выключает шедулер проектов.
- `PROJECTS_SCHEDULER_PAGE_SIZE` — сколько логинов считывается за одну страницу из БД.
- `PROJECTS_SCHEDULER_BATCH_SIZE` — размер батча логинов в одном шаге обработки.
- `PROJECTS_SCHEDULER_GRAPHQL_BATCH_SIZE` — размер батча для кампусов с GraphQL провайдером проектов.
- `PROJECTS_SCHEDULER_REST_BATCH_SIZE` — размер батча для кампусов с REST провайдером проектов.
- `PROJECTS_SCHEDULER_CONCURRENCY` — размер пула потоков шедулера проектов.
- `PROJECTS_SCHEDULER_TASK_TIMEOUT` — timeout одной задачи обновления проектов по логину.
- `PROJECTS_SCHEDULER_RETRY_ATTEMPTS` — число попыток для failed/timeout задач.
- `PROJECTS_SCHEDULER_RETRY_BACKOFF` — пауза между retry-попытками.
- `PROJECTS_SCHEDULER_GRAPHQL_SCHOOL_ID` — schoolId кампуса, который принудительно идёт через GraphQL provider.

Параметры TTL:

- `PROJECTS_REFRESH_TTL` — единый TTL свежести проектов.
- `GRAPHQL_PROJECTS_REFRESH_TTL` — fallback TTL для проектов (используется, если `PROJECTS_REFRESH_TTL` не задан).
- `COALITION_REFRESH_TTL` — TTL обновления коалиций.
- `GRAPHQL_COALITION_REFRESH_TTL` — fallback TTL коалиций.

Параметры HTTP-клиента:

- `API_CLIENT_CONNECT_TIMEOUT` — timeout установления соединения.
- `API_CLIENT_READ_TIMEOUT` — timeout чтения ответа.
- `API_CLIENT_CALL_TIMEOUT` — общий timeout всего HTTP-вызова.

Параметры rate limit/retry для REST проектов:

- `PROJECTS_REST_LIMIT_FOR_PERIOD` — сколько REST-запросов к projects разрешено за период.
- `PROJECTS_REST_LIMIT_REFRESH_PERIOD` — период rate limiter (обычно `1s`).
- `PROJECTS_REST_TIMEOUT_DURATION` — сколько ждать слот rate limiter перед ошибкой.
- `PROJECTS_REST_RETRY_MAX_ATTEMPTS` — число retry попыток при ошибках REST.
- `PROJECTS_REST_RETRY_WAIT_DURATION` — пауза между retry попытками REST.

Глобальный лимит внешнего API (общий бюджет для REST + GraphQL):

- `EXTERNAL_GLOBAL_LIMIT_FOR_PERIOD` — общий лимит запросов к внешнему API за период.
- `EXTERNAL_GLOBAL_LIMIT_REFRESH_PERIOD` — период общего лимитера.
- `EXTERNAL_GLOBAL_TIMEOUT_DURATION` — сколько ждать слот общего лимитера.
- `EXTERNAL_GLOBAL_RETRY_MAX_ATTEMPTS` — retry попытки для общего лимитера.
- `EXTERNAL_GLOBAL_RETRY_WAIT_DURATION` — пауза между retry попытками общего лимитера.

Лимит парсинга кампусов/кластеров (workplace):

- `CAMPUS_WORKPLACE_LIMIT_FOR_PERIOD` — лимит запросов workplace-парсинга за период.
- `CAMPUS_WORKPLACE_LIMIT_REFRESH_PERIOD` — период лимитера workplace-парсинга.
- `CAMPUS_WORKPLACE_TIMEOUT_DURATION` — ожидание слота для workplace-парсинга.
- `CAMPUS_WORKPLACE_RETRY_MAX_ATTEMPTS` — retry попытки для workplace-парсинга.
- `CAMPUS_WORKPLACE_RETRY_WAIT_DURATION` — пауза между retry попытками workplace-парсинга.

Параметры rate limit для GraphQL:

- `GRAPHQL_GLOBAL_LIMIT_FOR_PERIOD` — глобальный лимит на все GraphQL-вызовы `s21edu`.
- `GRAPHQL_GLOBAL_LIMIT_REFRESH_PERIOD` — период глобального GraphQL limiter.
- `GRAPHQL_GLOBAL_TIMEOUT_DURATION` — сколько ждать слот глобального limiter.
- `GRAPHQL_PROJECTS_LIMIT_FOR_PERIOD` — локальный лимит GraphQL вызовов проектов.
- `GRAPHQL_CREDENTIALS_LIMIT_FOR_PERIOD` — локальный лимит GraphQL вызовов credentials.

Важно: фактический GraphQL throughput определяется как минимум из локального лимита метода и `GRAPHQL_GLOBAL_LIMIT_FOR_PERIOD`.

Текущие рабочие значения в `env/test/s21edu.env`:

- `PROJECTS_SCHEDULER_ENABLED=true`
- `PROJECTS_SCHEDULER_PAGE_SIZE=1000`
- `PROJECTS_SCHEDULER_BATCH_SIZE=20`
- `PROJECTS_SCHEDULER_GRAPHQL_BATCH_SIZE=24`
- `PROJECTS_SCHEDULER_REST_BATCH_SIZE=14`
- `PROJECTS_SCHEDULER_CONCURRENCY=7`
- `PROJECTS_SCHEDULER_TASK_TIMEOUT=PT75S`
- `PROJECTS_SCHEDULER_RETRY_ATTEMPTS=1`
- `PROJECTS_SCHEDULER_RETRY_BACKOFF=PT1S`
- `PROJECTS_REST_LIMIT_FOR_PERIOD=5`
- `PROJECTS_REST_LIMIT_REFRESH_PERIOD=1s`
- `EXTERNAL_GLOBAL_LIMIT_FOR_PERIOD=10`
- `EXTERNAL_GLOBAL_LIMIT_REFRESH_PERIOD=1s`
- `PROJECTS_REFRESH_TTL=PT1H`
- `GRAPHQL_PROJECTS_REFRESH_TTL=PT1H`

## Запуск тестов и покрытие

```bash
./gradlew runAllTests
./gradlew runAllTestsWithCoverage
```

Покрытие формируется в JaCoCo-отчетах по модулям:

- `<module>/build/reports/jacoco/test/html/index.html`
- `<module>/build/reports/jacoco/test/jacocoTestReport.xml`

## CI/CD

Основные workflow в GitHub Actions:

- `Test PR` — проверка тестов для PR в `develop`/`master`.
- `Secret Scan PR` — сканирование репозитория на секреты (`gitleaks`).
- `Build Images` — сборка и публикация Docker-образов в GHCR на `master`.
- `Sonar Push` — запуск Sonar-анализа на `master`.
- `Deploy Test` / `Deploy Prod` — ручные deploy workflow.

### Версионирование образов

В проекте включено автоматическое версионирование образов:

- версия хранится в `gradle.properties` (`appVersion`);
- при `push` в `master` версия увеличивается (patch);
- создается Git tag вида `vX.Y.Z`;
- Docker-образы публикуются с тегом `vX.Y.Z` (и дополнительно `latest`).

### Правила тегов и релизов

- Источник версии: `gradle.properties` -> `appVersion`.
- Повышение версии (patch) происходит автоматически при `push` в `master`.
- Каждый релиз получает git tag формата `vX.Y.Z`.
- Для деплоя используется конкретный тег релиза (`vX.Y.Z`), а не `latest`.
- Откат выполняется повторным деплоем предыдущего тега (`vX.Y.Z`).

Пример:

- был релиз `v0.1.7`
- новый `merge` в `master` -> `v0.1.8`
- если с `v0.1.8` проблема, деплой возвращается на `v0.1.7`

## Мониторинг

Через `docker-compose` поднимаются:

- `Prometheus` — сбор метрик.
- `Grafana` — дашборды и визуализация.

Сервисы публикуют health/metrics endpoint через Spring Actuator.

Подробности:

- [GitHub Actions и деплой](docs/github-actions.md)
- [Мониторинг и дашборды](docs/monitoring.md)
- [Локальная разработка через dev.sh](docs/dev-local.md)

## Безопасность и секреты

- Реальные секреты не должны попадать в git.
- Коммитятся только шаблоны `env/*/*.env.example`.
- Рабочие `.env` файлы игнорируются в `.gitignore`.
- Для CI/CD используются GitHub Secrets/Environment Secrets.

## Что демонстрирует этот проект

- Проектирование многомодульной архитектуры на Java/Spring.
- Интеграцию нескольких внешних систем (Telegram, Rocket.Chat, API кампуса).
- Построение CI/CD пайплайна с тестами, покрытием, анализом качества и публикацией Docker-образов.
- Организацию наблюдаемости (метрики, мониторинг).
- Работу с production-подобным окружением через контейнеризацию и управляемый deploy.
