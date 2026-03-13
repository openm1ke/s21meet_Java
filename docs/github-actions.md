# GitHub Actions и GHCR

## Какие workflow настроены

- `Test Push` (`.github/workflows/test.yml`)
- Запускается на `push` в `feature/**`.
- Выполняет модульные тесты (`_tests-reusable.yml`).

- `Test PR` (`.github/workflows/test-pr.yml`)
- Запускается на PR в `develop` и `master`.
- Выполняет тесты и Sonar-анализ через `sonar-common.yml`.

- `Secret Scan PR` (`.github/workflows/gitleaks-pr.yml`)
- Запускается на PR в `develop` и `master`.
- Ищет утечки секретов (`gitleaks`).

- `Build Images` (`.github/workflows/build-image.yml`)
- Запускается на `push` в `master` и вручную.
- Перед сборкой выполняет тесты.
- Автоматически повышает `appVersion` (patch), создаёт git tag `vX.Y.Z`.
- Публикует образы в `ghcr.io/<owner>/s21meet` с тегами `vX.Y.Z` и `latest`.

- `Sonar Push` (`.github/workflows/sonar-push.yml`)
- Запускается на `push` в `master`.
- Выполняет Sonar-анализ для основной ветки.

- `Deploy Test` (`.github/workflows/deploy-test.yml`)
- Ручной деплой в тестовое окружение.
- Использует стратегию `recreate-all` и профиль `infra`.

- `Deploy Prod` (`.github/workflows/deploy-prod.yml`)
- Ручной деплой в production.
- Использует стратегию `rolling`.

## Необходимые переменные и секреты GitHub

Переменные окружения (`test`/`prod`):

- `DEPLOY_HOST` — адрес сервера.
- `DEPLOY_USER` — пользователь SSH.
- `DEPLOY_APP_DIR` — каталог приложения на сервере (опционально).
- `DEPLOY_PORT` — SSH-порт (опционально, по умолчанию `22`).
- `DEPLOY_KNOWN_HOSTS` — known_hosts (опционально).

Секреты окружения:

- `DEPLOY_SSH_KEY` — приватный SSH-ключ.
- `SONAR_TOKEN` — токен Sonar (для Sonar workflow).

Переменные репозитория для Sonar:

- `SONAR_HOST_URL`
- `SONAR_ORGANIZATION`
- `SONAR_PROJECT_KEY`

## Что копируется на сервер при деплое

Workflow деплоя копируют:

- `docker-compose.yml`
- `scripts/deploy-compose.sh`
- `monitoring/prometheus/prometheus.yml` (для test deploy)
- `monitoring/grafana/provisioning` (для test deploy)

Runtime env-файлы должны существовать на сервере заранее:

- `env/test/compose.env`, `env/test/*.env` для test
- `env/prod/compose.env`, `env/prod/*.env` для prod

## Типовой процесс релиза

1. Разработка в `feature/*` -> автотесты (`Test Push`).
2. PR в `develop`/`master` -> `Test PR` + `Secret Scan PR`.
3. Merge в `master` -> `Build Images` и публикация образов в GHCR.
4. Ручной запуск `Deploy Test` с нужным `image_tag`.
5. После проверки — `Deploy Prod` с тем же `image_tag`.
