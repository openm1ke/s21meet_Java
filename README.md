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

### 5. Параметры окружения (сверено с `application.yml`)

Все параметры берутся из:

- `env/test/*.env.example` — шаблоны для локального/тестового окружения.

#### `compose.env` (оркестрация Docker Compose)

- `APP_ENV` — имя окружения для env-файлов (в текущей схеме используется `test`).
- `TZ` — timezone контейнеров.
- `IMAGE_REPO`, `IMAGE_TAG` — реестр и тег Docker-образов сервисов.
- `PROXY_MODE` — профиль прокси (`vless`/`ssh`) для Telegram.
- `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` — учетка Grafana.
- `POSTGRES_BIND_ADDR`, `S21AUTH_BIND_ADDR`, `S21EDU_BIND_ADDR`, `S21BOT_BIND_ADDR`, `S21ROCKET_BIND_ADDR`, `S21WEB_BIND_ADDR`, `PROMETHEUS_BIND_ADDR`, `GRAFANA_BIND_ADDR` — на каком адресе хоста публиковать порты сервисов.
- `WEB_EDGE_HTTP_BIND_ADDR`, `WEB_EDGE_HTTPS_BIND_ADDR` — bind-адреса reverse-proxy `web-edge` (Caddy) на портах 80/443.
- `WEB_PUBLIC_DOMAIN` — домен, который `web-edge` обслуживает по HTTPS и проксирует в `s21web`.
- `WEB_TLS_MODE` — режим TLS для `web-edge`: `off` (локальный tunnel-режим), `internal` (локальный HTTPS с self-signed) или `acme` (Let's Encrypt для деплоя).
- `WEB_TLS_EMAIL` — email для ACME-регистрации (обязателен при `WEB_TLS_MODE=acme`).
- `XRAY_CONFIG_FILE` — путь к конфигу xray-клиента.
- `SSH_TUNNEL_HOST`, `SSH_TUNNEL_PORT`, `SSH_TUNNEL_USER`, `SSH_TUNNEL_SOCKS_PORT` — параметры SSH SOCKS туннеля.
- `SSH_TUNNEL_KEY_FILE`, `SSH_TUNNEL_KNOWN_HOSTS_FILE` — пути к ключу и known_hosts для SSH-туннеля.

#### `postgres.env`

- `POSTGRES_DB` — имя базы.
- `POSTGRES_USER` — пользователь базы.
- `POSTGRES_PASSWORD` — пароль пользователя.

#### `s21auth.env`

- `EDU_LOGIN`, `EDU_PASS` — учетные данные внешнего API кампуса.
- `CRYPTO_KEY_BASE64` — ключ шифрования/дешифрования токена.
- `TOKEN_URI` — endpoint выдачи токена во внешнем API.

#### `s21bot.env`

- `BOT_TOKEN` — токен Telegram-бота.
- `BOT_ADMIN` — Telegram id администратора.
- `BOT_GROUP` — id/username целевой группы.
- `BOT_GROUP_INVITE_LINK` — инвайт-ссылка в группу.
- `BOT_WEB_APP_URL` — публичный HTTPS URL Web App (должен совпадать с доменом `WEB_PUBLIC_DOMAIN`).
- `BOT_PROXY_ENABLED`, `BOT_PROXY_TYPE`, `BOT_PROXY_HOST`, `BOT_PROXY_PORT` — параметры прокси для Telegram API.
- `PROFILE_SERVICE_URL` — URL сервиса профилей (`s21edu`).
- `ROCKETCHAT_SERVICE_URL` — URL сервиса Rocket.Chat (`s21rocket`).

#### `s21rocket.env`

- `ROCKET_CHAT_TOKEN` — токен бота Rocket.Chat.
- `ROCKET_CHAT_URL` — URL Rocket.Chat websocket/api.
- `ROCKET_CHAT_QR_BOT` — username бота для QR-верификации.
- `ROCKET_CHAT_QR_TIMEOUT` — timeout ожидания QR (сек).
- `ROCKET_CHAT_MESSAGE_TIMEOUT` — timeout отправки сообщения (сек).

#### `s21web.env`

- `PROFILE_SERVICE_URL` — URL `s21edu` для API веб-модуля.
- `BOT_TOKEN` — токен Telegram-бота для проверки `initData` из Mini App.
- `TELEGRAM_WEBAPP_AUTH_ENABLED` — включение проверки подписи запросов Telegram Web App.

#### `s21edu.env` (основная бизнес-конфигурация)

Подключение к БД и сервисам:

- `DB_URL`, `DB_USER`, `DB_PASSWORD` — JDBC URL и креды БД.
- `TOKEN_ENDPOINT` — URL `s21auth` для получения токена.
- `BOT_SERVICE_URL` — URL `s21bot` для уведомлений.
- `DDL_AUTO` — режим Hibernate schema validation/update.

Флаги и таймауты API:

- `API_CLIENT_ENABLED` — включает HTTP client к внешнему API.
- `API_CLIENT_CONNECT_TIMEOUT`, `API_CLIENT_READ_TIMEOUT`, `API_CLIENT_CALL_TIMEOUT` — connect/read/call timeout.
- `GRAPHQL_API_ENABLED`, `CAMPUS_API_ENABLED`, `CLUSTER_API_ENABLED`, `PARTICIPANT_API_ENABLED`, `COALITION_API_ENABLED` — включение отдельных внешних API направлений.

Провайдеры и TTL:

- `COALITION_PROVIDER` — выбор провайдера коалиций (`auto|graphql|rest`).
- `COALITION_REFRESH_TTL` — TTL данных коалиций.
- `COALITION_REST_FETCH_MEMBER_COUNT` — догружать ли count участников через REST.
- `COALITION_REST_PAGE_SIZE` — page size REST-коалиций.
- `PROJECTS_REFRESH_TTL` — TTL проектов.
- `GRAPHQL_COALITION_REFRESH_TTL`, `GRAPHQL_PROJECTS_REFRESH_TTL` — fallback TTL (если базовые TTL не заданы).
- `PROJECTS_ROUTING_GRAPHQL_SCHOOL_ID` — campus/school id, принудительно идущий через GraphQL для проектов.
- `PROJECTS_REST_PAGE_SIZE` — page size REST-проектов.
- `CAMPUS_WORKPLACE_PROVIDER` — провайдер workplace (`graphql` или `rest`).

Настройки scheduler-пулов:

- `SCHEDULER_POOL_SIZE`, `SCHEDULER_THREAD_NAME_PREFIX` — общий spring scheduling pool.
- `PROJECTS_SCHEDULER_ENABLED`, `PROJECTS_SCHEDULER_INITIAL_DELAY`, `PROJECTS_SCHEDULER_FIXED_DELAY` — включение и период запуска sync проектов.
- `PROJECTS_SCHEDULER_MAX_LOGINS_PER_RUN` — лимит логинов за один прогон.
- `PROJECTS_SCHEDULER_PAGE_SIZE` — размер выборки stale credentials из БД.
- `PROJECTS_SCHEDULER_BATCH_SIZE` — базовый размер батча.
- `PROJECTS_SCHEDULER_GRAPHQL_BATCH_SIZE`, `PROJECTS_SCHEDULER_REST_BATCH_SIZE` — батчи для GraphQL/REST кампусов.
- `PROJECTS_SCHEDULER_CONCURRENCY` — параллелизм задач обновления проектов.
- `PROJECTS_SCHEDULER_TASK_TIMEOUT` — timeout одной задачи.
- `PROJECTS_SCHEDULER_RETRY_ATTEMPTS`, `PROJECTS_SCHEDULER_RETRY_BACKOFF` — retry и backoff для failed/timeout обновлений.
- `PROJECTS_SCHEDULER_GRAPHQL_SCHOOL_ID` — legacy fallback id для GraphQL routing (используется как запасной источник).
- `CREDENTIALS_SCHEDULER_ENABLED`, `CREDENTIALS_SCHEDULER_CRON`, `CREDENTIALS_SCHEDULER_ZONE` — включение и расписание sync credentials.
- `CREDENTIALS_SCHEDULER_PAGE_SIZE`, `CREDENTIALS_SCHEDULER_BATCH_SIZE`, `CREDENTIALS_SCHEDULER_CONCURRENCY` — параметры batch обработки credentials.

Rate limit / retry resilience4j:

- `PLATFORM_RETRY_MAX_ATTEMPTS`, `PLATFORM_RETRY_WAIT_DURATION`, `PLATFORM_RETRY_EXPONENTIAL_BACKOFF`, `PLATFORM_RETRY_EXPONENTIAL_MULTIPLIER` — общий retry внешнего platform API.
- `EXTERNAL_GLOBAL_LIMIT_FOR_PERIOD`, `EXTERNAL_GLOBAL_LIMIT_REFRESH_PERIOD`, `EXTERNAL_GLOBAL_TIMEOUT_DURATION` — глобальный limiter на внешний API.
- `EXTERNAL_GLOBAL_RETRY_MAX_ATTEMPTS`, `EXTERNAL_GLOBAL_RETRY_WAIT_DURATION`, `EXTERNAL_GLOBAL_RETRY_EXPONENTIAL_BACKOFF`, `EXTERNAL_GLOBAL_RETRY_EXPONENTIAL_MULTIPLIER` — retry для глобального external limiter.
- `CAMPUS_WORKPLACE_LIMIT_FOR_PERIOD`, `CAMPUS_WORKPLACE_LIMIT_REFRESH_PERIOD`, `CAMPUS_WORKPLACE_TIMEOUT_DURATION` — limiter workplace-парсинга.
- `CAMPUS_WORKPLACE_RETRY_MAX_ATTEMPTS`, `CAMPUS_WORKPLACE_RETRY_WAIT_DURATION`, `CAMPUS_WORKPLACE_RETRY_EXPONENTIAL_BACKOFF`, `CAMPUS_WORKPLACE_RETRY_EXPONENTIAL_MULTIPLIER` — retry workplace-парсинга.
- `GRAPHQL_CREDENTIALS_LIMIT_FOR_PERIOD`, `GRAPHQL_CREDENTIALS_LIMIT_REFRESH_PERIOD`, `GRAPHQL_CREDENTIALS_TIMEOUT_DURATION` — limiter GraphQL credentials.
- `GRAPHQL_CREDENTIALS_RETRY_MAX_ATTEMPTS`, `GRAPHQL_CREDENTIALS_RETRY_WAIT_DURATION` — retry GraphQL credentials.
- `GRAPHQL_PROJECTS_LIMIT_FOR_PERIOD`, `GRAPHQL_PROJECTS_LIMIT_REFRESH_PERIOD`, `GRAPHQL_PROJECTS_TIMEOUT_DURATION` — limiter GraphQL projects.
- `GRAPHQL_PROJECTS_RETRY_MAX_ATTEMPTS`, `GRAPHQL_PROJECTS_RETRY_WAIT_DURATION` — retry GraphQL projects.
- `GRAPHQL_GLOBAL_LIMIT_FOR_PERIOD`, `GRAPHQL_GLOBAL_LIMIT_REFRESH_PERIOD`, `GRAPHQL_GLOBAL_TIMEOUT_DURATION` — общий limiter всех GraphQL вызовов.
- `GRAPHQL_GLOBAL_RETRY_MAX_ATTEMPTS`, `GRAPHQL_GLOBAL_RETRY_WAIT_DURATION`, `GRAPHQL_GLOBAL_RETRY_EXPONENTIAL_BACKOFF`, `GRAPHQL_GLOBAL_RETRY_EXPONENTIAL_MULTIPLIER` — retry GraphQL global limiter.
- `PROJECTS_REST_LIMIT_FOR_PERIOD`, `PROJECTS_REST_LIMIT_REFRESH_PERIOD`, `PROJECTS_REST_TIMEOUT_DURATION` — limiter REST-проектов.
- `PROJECTS_REST_RETRY_MAX_ATTEMPTS`, `PROJECTS_REST_RETRY_WAIT_DURATION`, `PROJECTS_REST_RETRY_EXPONENTIAL_BACKOFF`, `PROJECTS_REST_RETRY_EXPONENTIAL_MULTIPLIER` — retry REST-проектов.

#### Результат сверки `env` ↔ `application.yml`

- Все обязательные переменные из `application.yml` покрыты шаблонами `env/test/*.env.example`.
- Переменные `DB_NAME`, `DB_PORT` присутствуют в `s21edu.env.example`, но напрямую не читаются `application.yml` (используются косвенно, если собираете `DB_URL` из частей).
- Для `s21web` есть опциональные параметры с дефолтами в `application.yml`, которых нет в env-шаблоне:
  - `PROJECT_EXECUTORS_RATE_LIMIT_ENABLED`
  - `PROJECT_EXECUTORS_RATE_LIMIT_FOR_PERIOD`
  - `PROJECT_EXECUTORS_RATE_LIMIT_REFRESH_PERIOD`
  По умолчанию фильтр включен и лимит составляет `60` запросов за `PT1M`.

### HTTPS для `s21web` через `web-edge` (Caddy)

- В `docker-compose.yml` добавлен сервис `web-edge`, который принимает HTTPS на 443 и проксирует в `s21web:8085`.
- Для локальной разработки: `WEB_TLS_MODE=internal`, `WEB_PUBLIC_DOMAIN=localhost`.
- Для деплоя: `WEB_TLS_MODE=acme`, `WEB_PUBLIC_DOMAIN=<ваш_домен>`, `WEB_TLS_EMAIL=<email>`.
- `BOT_WEB_APP_URL` в `s21bot.env` должен быть вида `https://<тот_же_домен>/`.
- Для Telegram Mini App домен нужно зарегистрировать в BotFather через `/setdomain`.

#### Актуальные параметры для текущего деплоя (`s21meet.ru`)

- `env/test/compose.env`:
  - `WEB_PUBLIC_DOMAIN=s21meet.ru`
  - `WEB_TLS_MODE=acme`
  - `WEB_TLS_EMAIL=admin@s21meet.ru` (замените на рабочий email)
- `env/test/s21bot.env`:
  - `BOT_WEB_APP_URL=https://s21meet.ru/`
- `env/test/s21web.env`:
  - `TELEGRAM_WEBAPP_AUTH_ENABLED=true`

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
- `Deploy Test` — ручной deploy workflow.

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
