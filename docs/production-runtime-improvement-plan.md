# План улучшения production-like runtime и backend

Дата составления: 2026-10-13

## Область плана

План составлен по результатам read-only проверки Docker-контура на `62.173.149.30:2222`. В репозитории сейчас настроено только GitHub environment `test`; отдельные production variables не обнаружены. Контур на сервере использует образы приложений `v0.1.6`, запущенные около двух месяцев.

Ограничения:

- сохраняем Spring Boot 3 и Java 21;
- не меняем секреты в Git и не добавляем credentials в репозиторий;
- сначала исправляем код и конфигурацию локально, затем собираем и проверяем образы;
- production deploy выполняется только после отдельной проверки и согласования результата.

## Зафиксированные наблюдения

1. Все контейнеры были `running`, Spring-сервисы и PostgreSQL — `healthy`, перезапусков и OOM не было.
2. `s21edu` временно получал `403 Forbidden` от внешней платформы примерно с 12:13 до 12:37. Ответ был HTML, хотя код ожидал JSON.
3. `s21bot` получал связанные 403 через `s21edu`.
4. После 12:38 внешние запросы снова стали успешными. Endpoint выдачи токена и запросы к платформе с текущим токеном возвращали `200`.
5. На сервере опубликованы наружу PostgreSQL `5432` и Grafana `3001`; в логах PostgreSQL видны внешние сканирования и неудачные попытки авторизации.
6. На хосте 3.7 ГБ RAM, доступно около 0.67 ГБ, swap почти заполнен.
7. В репозитории уже есть более новые версии Compose/мониторинга, чем запущены на сервере.

## Приоритет P0: устойчивость к отказам внешней платформы

### 1. Убрать повторные запросы для 401/403/404

Файл: `s21edu/src/main/resources/application.yml`.

Что изменить:

- заменить retry по базовому `PlatformClientException` на retry только для `PlatformRateLimitException`, `PlatformTransientException` и ограниченных сетевых ошибок;
- исключить `PlatformUnauthorizedException` и `PlatformNotFoundException` из retry;
- отдельно проверить настройки `externalGlobal`, `campusWorkplace` и `projectsRest`.

Зачем: временный 403 не должен порождать дополнительные запросы и усиливать возможную блокировку внешним WAF.

### 2. Сохранить исходный тип ошибки событий

Файл: `s21edu/src/main/java/ru/izpz/edu/client/EventClient.java`.

Что изменить:

- добавить отдельную обработку `PlatformClientException` с повторным выбросом исходного исключения;
- оборачивать только неизвестные `RuntimeException`;
- не логировать ожидаемую ошибку платформы как `Unexpected error`.

Зачем: сохранить HTTP-код, тип ошибки и корректную работу метрик/circuit breaker.

### 3. Безопасно обрабатывать не-JSON ответы

Файлы:

- `s21edu/src/main/java/ru/izpz/edu/exception/GlobalExceptionHandler.java`;
- `s21edu/src/main/java/ru/izpz/edu/client/RestPlatformApiFacade.java`.

Что изменить:

- обрабатывать `PlatformUnauthorizedException` отдельно;
- не передавать HTML или полный response body в лог;
- логировать только статус, тип ответа, endpoint/операцию и correlation id;
- возвращать единый безопасный DTO для 401/403/502.

Зачем: убрать вторичный Jackson stack trace и не допускать утечки внешнего содержимого ответа в логи.

## Приоритет P1: токены и внешний API

### 4. Проверить жизненный цикл service-token

Файлы:

- `s21edu/src/main/java/ru/izpz/edu/service/TokenService.java`;
- `s21auth/src/main/java/ru/izpz/auth/scheduler/TokenScheduler.java`;
- `s21auth/src/main/java/ru/izpz/auth/client/TokenClient.java`.

План:

- добавить метрики и структурированные логи: запрос токена, успешное обновление, возраст токена, ошибки выдачи;
- не логировать части access token;
- проверить, что обновление токена выполняется до истечения срока действия;
- при 401/403 один раз принудительно запросить свежий токен, затем открыть circuit breaker;
- проверить, нужен ли локальный кэш в `s21edu`, чтобы не запрашивать token endpoint на каждый внешний вызов.

Секреты `EDU_LOGIN`, `EDU_PASS`, `TOKEN_URI`, `CRYPTO_KEY_BASE64` должны оставаться только на сервере/в secret storage.

### 5. Добавить диагностику внешнего API

Файл: `s21edu/src/main/java/ru/izpz/edu/client/RestPlatformApiFacade.java` и конфигурация Actuator/Micrometer.

Добавить метрики по операции и статусу:

- количество 401/403/404/429/5xx;
- latency внешнего API;
- количество открытий circuit breaker;
- время последнего успешного обновления событий и профилей.

Создать алерты на устойчивую серию 403 и отсутствие успешного вызова событий в течение заданного окна.

## Приоритет P1: сетевой и инфраструктурный периметр

### 6. Закрыть внутренние порты

Файлы:

- `docker-compose.yml`;
- серверный `env`-профиль в `/home/appdeploy/app`;
- firewall/security group сервера.

Что сделать:

- оставить PostgreSQL на `127.0.0.1:5432` или полностью убрать host port;
- ограничить Grafana `127.0.0.1:3001` либо закрыть её за authenticated reverse proxy/VPN;
- проверить, что порты `8081–8084`, `9090` и H2 не доступны извне;
- добавить регулярную проверку открытых портов в deployment smoke check.

Текущие ошибки PostgreSQL от внешних сканеров не являются причиной 403 платформы, но показывают лишнюю экспозицию.

### 7. Проверить egress и allowlist внешней платформы

На сервере и у владельца внешней платформы проверить:

- публичный исходящий IP `62.173.149.30`;
- наличие временного rate limit/WAF block;
- лимиты для событий, профилей и GraphQL;
- необходимость allowlist или отдельного egress IP.

Критерий: при успешной выдаче токена внешний API не должен возвращать HTML 403 на корректные API-запросы.

## Приоритет P1: ресурсы хоста

### 8. Снизить риск memory pressure

На сервере проверить:

- `docker stats` по каждому контейнеру;
- JVM heap и `JAVA_TOOL_OPTIONS`;
- потребление PostgreSQL, Grafana и Prometheus;
- причины заполнения swap.

После измерений выбрать одно из решений:

- увеличить RAM/swap;
- задать явные JVM limits;
- уменьшить retention Prometheus;
- ограничить ресурсы инфраструктурных контейнеров.

Изменять лимиты без замеров нельзя: сейчас OOM не зафиксирован.

## Приоритет P2: актуальность deployment

### 9. Развести версии и окружения

Файлы:

- `.github/workflows/deploy-test.yml`;
- `scripts/deploy-compose.sh`;
- `docs/github-actions.md`;
- `docker-compose.yml`.

Что сделать:

- добавить явно описанный production environment в GitHub только после согласования адреса и политики доступа;
- не использовать `test` variables для production;
- деплоить immutable `IMAGE_TAG` по commit SHA или release tag;
- перед обновлением выполнять мигратор и smoke checks;
- после обновления проверять версии образов, healthchecks, логи и успешный внешний API вызов.

Текущий сервер работает на старом `v0.1.6`, поэтому локальные изменения и новые зависимости сами по себе на нём не применяются.

## Приоритет P2: тесты и регрессии

Добавить или расширить тесты для:

- `RestPlatformApiFacadeTest`: 401/403 с JSON и HTML телом;
- `EventClientTest`: сохранение `PlatformUnauthorizedException`;
- `GlobalExceptionHandlerTest`: безопасный ответ для HTML 403;
- retry-конфигурации: 429/5xx повторяются, 401/403/404 — нет;
- интеграционного smoke test: token endpoint → внешний API → scheduler metrics.

Локальная проверка после внесения изменений:

```bash
./gradlew :s21edu:test :s21auth:test :s21bot:test
./gradlew checkstyleMain pmdMain spotbugsMain
./gradlew buildAllJars
```

## Порядок выполнения

1. Зафиксировать baseline: версии образов, healthchecks, открытые порты, memory usage и текущие метрики.
2. Исправить классификацию и обработку ошибок `s21edu`.
3. Уточнить retry/circuit breaker и добавить метрики 401/403/429/5xx.
4. Проверить token refresh и убрать логирование token fragments.
5. Закрыть PostgreSQL/Grafana и проверить firewall.
6. Снять memory profile и определить resource limits.
7. Прогнать тесты, статический анализ и локальный Docker contour.
8. Собрать новые образы с immutable tag.
9. Обновить сначала test contour, затем выполнить smoke/лог-проверки.
10. Только после подтверждения адреса production и результатов test contour выполнить production deploy.

## Критерии готовности

- нет повторных retry для 401/403/404;
- HTML/невалидный ответ внешнего API не создаёт Jackson stack trace и не попадает целиком в лог;
- токен обновляется заранее, его части не логируются;
- все сервисы healthy, restart count не растёт;
- PostgreSQL и Grafana не доступны из публичной сети без предусмотренного proxy/VPN;
- внешние вызовы имеют метрики и алерты;
- после обновления подтверждены актуальные image tags и успешные события/профили;
- production environment и процедура отката документированы.

До отдельного согласования этот файл является только планом: изменения в код, Compose, секреты и сервер не вносятся.
