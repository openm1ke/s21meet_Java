# План реализации P0: ошибки внешней платформы и безопасные retry

Дата анализа: 2026-10-19

Статус: реализация P0 для REST и GraphQL внесена в рабочее дерево, локальные тесты и Docker smoke-check пройдены. Изменения остаются незакоммиченными.

Выполнено в текущей итерации:

- REST-ошибки платформы разделены на постоянные, временные и транспортные;
- retry-конфигурация переведена на allowlist временных ошибок;
- `EventClient` сохраняет специализированные исключения;
- `GlobalExceptionHandler` безопасно обрабатывает HTML, пустые и повреждённые ответы без записи полного тела в лог;
- ручной retry проектов ограничен временными ошибками и timeout;
- GraphQL-ошибки разделены на постоянные, временные, rate-limit и transport-типы;
- GraphQL retry-конфигурации переведены на явный allowlist временных ошибок;
- добавлены unit-тесты и Spring integration-тест фактического количества попыток Resilience4j.

## 1. Цель

Устранить повторные запросы для постоянных ошибок внешней платформы и оставить ограниченные retry только для действительно временных сбоев. Одновременно сохранить исходный тип ошибки, не допустить разбора HTML как JSON и не писать полное тело ответа внешнего сервиса в лог.

Ожидаемое поведение:

- 401, 403, 404, 400 и 409 завершают текущий вызов без повторной отправки;
- 429 повторяется ограниченное число раз с backoff;
- 408, 500, 502, 503, 504 и сетевые timeout/connectivity errors повторяются ограниченно;
- неизвестная ошибка не становится автоматически retryable;
- исходный HTTP-код и тип исключения доходят до сервисов, scheduler и REST-обработчика;
- невалидный или HTML-ответ превращается в безопасный ответ приложения без Jackson stack trace и без публикации полного тела ответа.

## 2. Что проверено

### 2.1. Источник контракта внешней платформы

Рабочий Swagger — `openapi/swagger.json`. Он подключается в `common/build.gradle.kts` через OpenAPI Generator `7.25.0`; generated API-клиенты создаются в `build/generated` и не редактируются вручную. `openapi/old_swagger.json` является исторической копией и не участвует в текущей сборке.

В `openapi/swagger.json` указано:

- OpenAPI `3.0.1`;
- сервер `https://platform.21-school.ru/services/21-school/api`;
- авторизация через заголовок `Authorization`;
- REST-операции кампусов, кластеров, участников, проектов, коалиций и событий;
- для большинства операций описаны `401`, `403`, `404`, `429`, `500` и `400`;
- `403`, `404` и `500` описаны как JSON `ErrorResponseDTO`;
- `401` и `429` в Swagger не имеют описанного тела ответа;
- `ErrorResponseDTO` содержит обязательные поля `status`, `exceptionUUID`, `code`, `message`.

Фактически наблюдавшийся HTML `403` не соответствует JSON-контракту Swagger. Его нужно считать недоверенным внешним ответом и обрабатывать как отдельный fallback-сценарий.

### 2.2. Текущий REST-путь

Цепочка вызова сейчас выглядит так:

```text
controller/scheduler
  -> service/provider
  -> PlatformApiFacade
  -> RestPlatformApiFacade
  -> generated OpenAPI API client
  -> RestClient с Authorization interceptor
  -> внешняя платформа
```

`RestPlatformApiFacade` использует `@Retry(name = "externalGlobal")` почти для всех REST-методов:

| Метод фасада | Операция Swagger |
|---|---|
| `getClustersByCampus` | `GET /v1/campuses/{campusId}/clusters` |
| `getParticipantsByClusterId` | `GET /v1/clusters/{clusterId}/map` |
| `getParticipantsByCampusId` | `GET /v1/campuses/{campusId}/participants` |
| `getEvents` | `GET /v1/events` |
| `getParticipantProjectsByLogin` | `GET /v1/participants/{login}/projects` |
| `getCoalitionByLogin` | `GET /v1/participants/{login}/coalition` |
| `getParticipantsByCoalitionId` | `GET /v1/coalitions/{coalitionId}/participants` |
| `getParticipantByLogin` | `GET /v1/participants/{login}` |

Текущий `toPlatformException` уже разделяет `401/403`, `404`, `429` и `5xx`, но возвращает `PlatformUnauthorizedException` для обоих статусов `401` и `403`. Это допустимо для retry-классификации, если решение всегда дополнительно проверяет `getCode()`.

### 2.3. Найденные проблемы

1. В `s21edu/src/main/resources/application.yml` для `externalGlobal` указаны базовый `PlatformClientException`, `GraphQlRemoteException` и `RequestNotPermitted`. Базовый тип включает ошибки `401`, `403`, `404` и любые другие постоянные ошибки.
2. `campusWorkplace` и `projectsRest` также содержат общий `PlatformClientException`, хотя фактические аннотации у провайдеров различаются:
   - REST-провайдер workplace использует только `RateLimiter`, а retry фасада выполняется через `externalGlobal`;
   - GraphQL-провайдер workplace использует `campusWorkplace` и выбрасывает `GraphQlRemoteException`;
   - REST projects facade использует только `projectsRest` rate limiter, а scheduler имеет отдельный ручной retry.
3. `RestPlatformApiFacade` заворачивает `ResourceAccessException` и другие `RestClientException` в один базовый `PlatformClientException`. Из-за этого сетевой timeout нельзя отличить от ошибки декодирования или другого локального сбоя.
4. `EventClient` ловит любой `RuntimeException`, включая уже специализированные ошибки платформы, и заменяет его на `Unexpected error while fetching events`.
5. `GlobalExceptionHandler` пытается разобрать любое тело как `ServiceErrorDto`. При ошибке разбора он логирует полный `responseBody`; для HTML это даёт вторичный шум и потенциально раскрывает внешний ответ.
6. При транспортной ошибке код может быть `0`, поэтому перед возвратом HTTP-ответа нужно нормализовать статус в `502` или `503`, а не передавать некорректный статус дальше.
7. `StudentProjectsScheduler` повторяет любой результат `FAILED` независимо от исходной причины. `RestApiProjectsProvider` перед этим теряет тип `PlatformClientException`, превращая ошибку в общий `FetchResult.failure()`.
8. GraphQL имеет отдельную цепочку: `GraphQLApiClient` классифицирует HTTP, GraphQL и transport-ошибки отдельно от REST-классов. Полное тело ответа не попадает в сообщение исключения или логи.

## 3. Решения, которые нужно зафиксировать до кода

### 3.1. Retry строится по allowlist

В конфигурации перечисляются только типы ошибок, для которых разрешён retry. Базовый `PlatformClientException` туда не добавляется. Это безопаснее, чем пытаться перечислить все постоянные ошибки через `ignoreExceptions`.

`ignoreExceptions` можно добавить как дополнительный защитный слой для `PlatformUnauthorizedException` и `PlatformNotFoundException`, но нельзя добавлять туда базовый `PlatformClientException`, иначе будут подавлены и retryable-подклассы.

### 3.2. Ошибки разделяются на HTTP и транспортные

Сохраняются существующие типы:

- `PlatformUnauthorizedException` — `401` или `403`;
- `PlatformNotFoundException` — `404`;
- `PlatformRateLimitException` — `429`;
- `PlatformTransientException` — временные HTTP-ответы.

Для сетевых ошибок рекомендуется добавить `PlatformTransportException extends PlatformClientException`. Он будет использоваться только для connect timeout, read timeout, connection reset и других `ResourceAccessException`. Ошибки JSON-декодирования и неизвестные `RestClientException` должны оставаться non-retryable.

Если при реализации окажется, что отдельный класс не нужен, допустима альтернатива: расширить `PlatformTransientException` конструктором с причиной и специальным кодом `0`. Выбранный вариант должен быть единым для всех REST-методов и тестов.

### 3.3. Классификация HTTP-статусов

Рекомендуемая таблица:

| Статусы | Тип | Retry | Действие |
|---|---|---:|---|
| `400`, `401`, `403`, `404`, `409` | permanent client/access error | нет | вернуть исходный тип и код |
| `408` | transient HTTP error | да | ограниченный backoff |
| `429` | rate limit | да | ограниченные попытки, сохранить `Retry-After` |
| `500`, `502`, `503`, `504` | transient server error | да | ограниченный exponential backoff |
| остальные `4xx` | permanent client error | нет | общий `PlatformClientException` |
| остальные `5xx` | non-retryable server error до отдельного решения | нет | безопасный `502` наружу |
| timeout/connect/reset | transient transport error | да | ограниченный backoff |
| JSON decode/прочая локальная ошибка клиента | non-retryable | нет | безопасный `502` наружу |

Статусы `408`, `502`, `503` и `504` не перечислены во всех ответах текущего Swagger, но могут появиться на gateway, reverse proxy или промежуточном сетевом слое. Их нужно классифицировать по семантике ответа, а не только по текущему файлу спецификации.

## 4. План изменений

### Этап 0. Зафиксировать baseline

1. Сохранить текущие значения retry-параметров из `application.yml` и переменных окружения.
2. Проверить, какие провайдеры включены в local/test Compose:
   - `CAMPUS_WORKPLACE_PROVIDER`;
   - `COALITION_PROVIDER`;
   - `api.graphql.enabled`;
   - `api.participant.enabled`.
3. Зафиксировать фактическое число запросов для одного сбоя на:
   - `getEvents`;
   - `getParticipantByLogin`;
   - `getClustersByCampus`;
   - projects scheduler.
4. Не выполнять реальные запросы к production API из unit-тестов. Все ответы моделировать через `RestClientResponseException` и `ResourceAccessException`.

### Этап 1. Исправить классификацию в `RestPlatformApiFacade`

Файл: `s21edu/src/main/java/ru/izpz/edu/client/RestPlatformApiFacade.java`.

1. Оставить generated OpenAPI-клиенты и `ApiClientConfig` без ручного редактирования.
2. Вынести классификацию HTTP-ответа в отдельный private/package-level метод, чтобы проверить её без вызова сети.
3. Сохранить в исключении код, headers и ограниченное тело ответа для диагностики. Минимально необходимо сохранить `Retry-After`.
4. Для `ResourceAccessException` выбрасывать `PlatformTransportException`.
5. Для других `RestClientException` выбрасывать базовый non-retryable `PlatformClientException`.
6. Разделить retryable `5xx` по allowlist, а не использовать условие `code >= 500` для всех 5xx.
7. Не логировать response body в фасаде. Если нужна диагностика, писать только operation, status, content type, body length и correlation id.

### Этап 2. Исправить propagation в `EventClient`

Файл: `s21edu/src/main/java/ru/izpz/edu/client/EventClient.java`.

Изменить порядок обработки исключений:

```java
catch (PlatformClientException ex) {
  throw ex;
} catch (RuntimeException ex) {
  throw new PlatformClientException("Unexpected error while fetching events", ex);
}
```

Логировать ожидаемую ошибку платформы один раз на подходящем уровне. Не превращать `401`, `403`, `404`, `429` и `5xx` в общий `Unexpected error`.

### Этап 3. Перенастроить Resilience4j

Файл: `s21edu/src/main/resources/application.yml`.

Для `externalGlobal` оставить в `retryExceptions` только:

- `PlatformRateLimitException`;
- `PlatformTransientException`;
- `PlatformTransportException`;
- `RequestNotPermitted`, если проверка подтвердит, что локальный rate limiter должен иметь короткий retry.

Удалить из `externalGlobal`:

- `PlatformClientException`;
- `GraphQlRemoteException`.

Для `campusWorkplace` и `projectsRest`:

1. Сначала проверить фактический provider path.
2. Не оставлять общий `PlatformClientException` в retry-конфигурации.
3. Для REST workplace/projects полагаться на `externalGlobal`, который находится на `RestPlatformApiFacade`.
4. Для GraphQL используются собственные `GraphQlRateLimitException`, `GraphQlTransientException` и `GraphQlTransportException`; постоянные ошибки остаются базовым `GraphQlRemoteException` или специализированными access/not-found типами.
5. Для конфигураций, которые имеют `@Retry`, но не имеют явного `retryExceptions`, указать явную политику или отключить retry, чтобы поведение не зависело от defaults библиотеки.

Параметры первой версии:

- `maxAttempts`: 2 или 3 включая исходную попытку;
- exponential backoff с верхним пределом;
- общий retry budget меньше scheduler timeout;
- `Retry-After` сохраняется и логируется как число секунд, но полное тело ответа не логируется.

Динамический backoff по значению `Retry-After` можно реализовать через кастомный `IntervalBiFunction`, если текущих YAML-параметров окажется недостаточно. Это следует делать после базовой проверки allowlist, чтобы не смешивать две задачи.

### Этап 4. Сделать безопасный `GlobalExceptionHandler`

Файл: `s21edu/src/main/java/ru/izpz/edu/exception/GlobalExceptionHandler.java`.

1. Для `PlatformRateLimitException` возвращать `429` и короткое сообщение о временном ограничении.
2. Для `PlatformTransientException` и `PlatformTransportException` возвращать `502` или `503` по единому выбранному правилу.
3. Для `PlatformUnauthorizedException` использовать `getCode()`:
   - `401`: безопасное сообщение об отказе авторизации;
   - `403`: безопасное сообщение о запрете доступа.
4. Для `PlatformNotFoundException` возвращать `404`, не повторяя непроверенный внешний текст.
5. JSON разбирать только если тело непустое и действительно соответствует `ServiceErrorDto`.
6. Проверять, что `status` находится в допустимом HTTP-диапазоне и совпадает с кодом исключения; иначе использовать код исключения.
7. При HTML, пустом или повреждённом JSON возвращать безопасный DTO/map с `status` и общим `message`.
8. В лог писать только:
   - operation или endpoint, если он доступен;
   - HTTP status;
   - error class;
   - content type;
   - размер тела;
   - `exceptionUUID`, если JSON прошёл валидацию;
   - correlation id.
9. Никогда не писать `ex.getResponseBody()` целиком, даже на уровне `debug`.
10. Для кода `0` и неизвестного кода выбирать `502`, а не строить `ResponseEntity.status(0)`.

### Этап 5. Устранить скрытые ручные retry

Это обязательная проверка перед закрытием P0, потому что Resilience4j — не единственный механизм повторов.

1. В `StudentProjectsScheduler` результат `FAILED` сейчас повторяется независимо от причины.
2. В `RestApiProjectsProvider` тип `PlatformClientException` теряется в `FetchResult.failure()`.
3. Сохранить классификацию причины до уровня scheduler или добавить явный `isRetryable(Throwable)`:
   - retry: `PlatformRateLimitException`, `PlatformTransientException`, `PlatformTransportException`, timeout;
   - no retry: `PlatformUnauthorizedException`, `PlatformNotFoundException`, другие постоянные `4xx`;
   - отдельное решение для неизвестной ошибки.
4. Добавить тест, подтверждающий, что `403` не повторяется в projects scheduler, а `503` и timeout повторяются ограниченное число раз.

GraphQL retry не смешивается с REST retry: его allowlist и тесты проверяются отдельными конфигурациями `graphqlGlobal`, `graphqlCredentials`, `graphqlProjects` и `graphqlCoalition`.

## 5. Тестовый план

### 5.1. Unit-тесты `RestPlatformApiFacade`

Добавить проверки:

- `401` и `403` → `PlatformUnauthorizedException` с сохранением кода;
- `404` → `PlatformNotFoundException`;
- `429` → `PlatformRateLimitException` с `Retry-After`;
- `408`, `500`, `502`, `503`, `504` → retryable transient type;
- `501` или неизвестный `5xx` → non-retryable base type;
- `ResourceAccessException` → transport type;
- обычный `RestClientException` → non-retryable base type;
- headers без `Retry-After` и `null` headers не вызывают `NullPointerException`.

### 5.2. Unit-тесты `EventClient`

- специализированный `PlatformUnauthorizedException` выбрасывается тем же экземпляром;
- `PlatformNotFoundException`, `PlatformRateLimitException` и `PlatformTransientException` не теряют тип;
- неизвестный `RuntimeException` оборачивается в `PlatformClientException` с причиной;
- `null` response продолжает считаться ошибкой платформы.

### 5.3. Unit-тесты `GlobalExceptionHandler`

- валидный JSON `ErrorResponseDTO` возвращается с корректным статусом;
- HTML `403` возвращает безопасный ответ без HTML в body;
- повреждённый JSON `404` возвращает `404` и безопасное сообщение;
- `401` и `403` не раскрывают внешний message;
- `429` возвращает `429`;
- транспортная ошибка с кодом `0` возвращает `502` или `503` по принятому правилу;
- `null` или пустое тело не приводит к вторичному исключению.

Для проверки отсутствия утечки тела добавить Logback `ListAppender` либо отдельный тест helper-а, который формирует log fields. Проверять, что HTML и строка-маркер из response body отсутствуют в сообщении лога.

### 5.4. Retry integration tests

Тестировать прокси Spring, а не прямой вызов `new RestPlatformApiFacade(...)`, иначе аннотации Resilience4j не выполняются.

Минимальный набор:

- `401`, `403`, `404` вызывают generated API ровно один раз;
- `429` вызывает его не больше `maxAttempts`;
- `503` вызывает его не больше `maxAttempts`;
- transport timeout вызывает его не больше `maxAttempts`;
- после исчерпания попыток наружу выходит исходный специализированный тип;
- `campusWorkplace` и `projectsRest` проверяются в каждом реально используемом provider path;
- ручной projects scheduler не повторяет permanent errors.

Предпочтительный способ — отдельная Spring test configuration с `@EnableAspectJAutoProxy` и Resilience4j aspect. Если для конкретной версии библиотеки это окажется нестабильно, использовать небольшой `@SpringBootTest` только для `s21edu` с mock generated API и выключенными scheduler/database beans.

### 5.5. GraphQL classification tests

- HTTP `401/403` → `GraphQlUnauthorizedException` без retry;
- HTTP `404` → `GraphQlNotFoundException` без retry;
- HTTP `429` → `GraphQlRateLimitException` с ограниченным retry;
- HTTP `408/500/502/503/504` → `GraphQlTransientException` с ограниченным retry;
- `ResourceAccessException` → `GraphQlTransportException` с ограниченным retry;
- `extensions.code` классифицируется по allowlist, неизвестная ошибка не retry-ится;
- полное GraphQL response body не попадает в сообщение исключения.

## 6. Проверка Swagger и реализации после кода

Перед слиянием повторно сверить:

1. все методы `PlatformApiFacade` с `operationId` и path в `openapi/swagger.json`;
2. обязательные поля `ErrorResponseDTO`;
3. поведение generated клиента при ответах без тела для `401` и `429`;
4. обработку JSON `403/404/500`;
5. fallback для HTML `403`, который Swagger не описывает;
6. отсутствие ручных изменений в `build/generated`.

Если внешний Swagger изменится, сначала обновить `openapi/swagger.json`, выполнить генерацию common-модуля и проверить diff generated clients. В рамках этой задачи изменение Swagger-файла не требуется.

## 7. Порядок локальной проверки

После реализации выполнять последовательно:

```bash
./gradlew :common:openApiGenerate
./gradlew :s21edu:test
./gradlew :s21edu:checkstyleMain :s21edu:pmdMain :s21edu:spotbugsMain
./gradlew check
```

Затем проверить локальный Docker-контур:

```bash
scripts/local_contour_status.sh
scripts/quality_local.sh fast
docker compose config
```

Для Graphify после изменения поддерживаемых файлов использовать:

```bash
scripts/graphify_local_update.sh
```

В тестовом контейнере проверить четыре сценария:

1. успешный ответ внешней платформы;
2. JSON `403`;
3. HTML `403`;
4. `503`/timeout с ограниченным числом повторов.

В логах должны отсутствовать полный HTML, полный response body, access token и повторные попытки для `401/403/404`.

## 8. Порядок выкладки и откат

1. Сначала выполнить unit и integration tests локально.
2. Собрать test-образы с immutable tag по commit SHA.
3. Развернуть только test contour.
4. Проверить количество вызовов внешней платформы при искусственных `401`, `403`, `404`, `429` и `503`.
5. Проверить scheduler metrics и логи.
6. Сравнить задержку ответа до и после изменения.
7. Только после успешной проверки использовать образ в production.
8. При росте ошибок или неожиданном снижении успешных обновлений вернуть предыдущий image tag. Rollback не должен требовать миграции базы данных, так как задача не меняет схему.

## 9. Критерии готовности P0

- `401/403/404/400/409` не вызывают retry;
- `429`, разрешённые `5xx` и сетевые ошибки повторяются ограниченно;
- базовый `PlatformClientException` отсутствует в retry allowlist REST-контура;
- `EventClient` сохраняет специализированные исключения;
- HTML и повреждённый JSON не вызывают вторичного Jackson stack trace;
- полное тело ответа внешней платформы не появляется в логах;
- код `0` не используется как HTTP status ответа приложения;
- ручной retry projects scheduler не повторяет permanent errors;
- generated OpenAPI-клиенты остаются воспроизводимыми из `openapi/swagger.json`;
- тесты подтверждают фактическое количество внешних вызовов;
- локальный Docker contour healthy, а test deploy проходит smoke-check;
- rollback на предыдущий image tag проверен документированным способом.

## 10. Что не входит в этот P0

- обновление токена по отдельной стратегии при `401`;
- добавление новых метрик внешнего API и alert rules;
- изменение GraphQL-схемы или внешнего GraphQL-контракта;
- закрытие публичных портов PostgreSQL/Grafana;
- изменение Swagger внешней платформы;
- production deployment и изменение секретов.

Эти задачи остаются отдельными этапами из общего production runtime плана.
