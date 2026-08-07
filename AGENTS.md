# Project instructions for Codex

## Stack
- Java 21
- Spring Boot
- Gradle multi-module project
- PostgreSQL
- Liquibase
- Docker Compose
- GitHub Actions

## Rules
- Before changing code, inspect settings.gradle, root build.gradle/build.gradle.kts and module build files.
- Prefer minimal changes.
- Do not change secrets, tokens, production credentials or private keys.
- Do not rewrite unrelated files.
- For Java code, keep existing package structure and style.
- After code changes, suggest the exact Gradle command to verify the change.
- If possible, run relevant tests with ./gradlew test or a narrower module test task.

## Important areas
- Check common module before duplicating DTOs, exceptions or utilities.
- Check Liquibase migrations before changing entities or repositories.
- Be careful with Docker Compose and CI workflows.

## Local MCP

Project MCP configuration is in `.mcp.json`; detailed workflow is in `docs/mcp-local.md`.

Rules:
- Do not commit GitHub tokens, Postgres passwords, Docker credentials or other secrets. MCP wrappers read secrets from local environment variables or ignored `env/<profile>/*.env` files.
- Prefer `docker-readonly` for routine Docker inspection. Use `docker-compose-safe` for local test-contour compose operations; it disables destructive tools.
- Use `postgres-test` only against local/test databases. It is read-only and defaults to `env/${APP_ENV:-test}/postgres.env`.
- Use `playwright` for browser checks of `s21web` after the local web service is running.
- Docker Desktop must be running before Docker/GitHub container-backed MCP servers can start.
- Use `scripts/local_contour_status.sh` for a quick compose/health snapshot and `scripts/quality_local.sh fast` for local config/lint smoke checks.

## Graphify

В `graphify-out/` хранится локальный knowledge graph проекта с Java/Spring/Gradle AST-связями и community structure. Каталог генерируется локально, исключен из Git и общий для Codex, Claude и других агентов.

Если пользователь пишет `/graphify`, сначала используй установленный Graphify skill или эти инструкции.

Правила:
- Подробный локальный workflow описан в `docs/graphify-local.md`.
- Для нетривиальных вопросов о кодовой базе сначала запускай `graphify query "<question>"`, если существует `graphify-out/graph.json`. Формулируй запрос на английском с точными Java identifiers и domain terms, например `MessageProcessor ConfirmedFlow search flow`, `ProfileController checkEduLogin repository flow`, `Liquibase Profile entity changes`.
- Для связи двух сущностей используй `graphify path "<A>" "<B>"`, для локального контекста символа — `graphify explain "<concept>"`, для оценки затронутого кода перед рефакторингом — `graphify affected "<symbol>" --depth 2`.
- Graphify нужен для ориентации и impact analysis, но не заменяет чтение исходников. Перед изменением проверь актуальную реализацию, imports/usages и релевантные тесты через `rg`; граф может отставать от незакоммиченных изменений.
- Если существует `graphify-out/wiki/index.md`, используй его для широкой навигации. Если wiki отсутствует — используй `graphify query` и `GRAPH_REPORT.md` для ориентации. Не регенерируй wiki автоматически.
- Читай `graphify-out/GRAPH_REPORT.md` только для архитектурного обзора или когда `query`/`path`/`explain` недостаточно; не открывай его для локальных задач.
- Грязные файлы в `graphify-out/` ожидаемы и не являются причиной пропускать Graphify. Если граф отсутствует, Graphify недоступен или задача относится к ошибочному/устаревшему графу, продолжай с точечным `rg`.
- После изменения поддерживаемого кода (`.java`, `.kts`, `.yaml`, `.yml`, `.sql`, Docker/CI/config files) запускай `scripts/graphify_local_update.sh`. Скрипт делает локальный `graphify update .` и `graphify cluster-only . --no-label` без LLM/API-затрат и очищает cloud LLM env vars для дочернего процесса.
- Для обновления человекочитаемых community labels используй только `scripts/graphify_local_label.sh`; он работает через локальный Ollama backend, сам запускает Ollama при необходимости, скачивает отсутствующую модель и останавливает только свой временный Ollama-процесс. Cloud API не используй.
- Для редкого полного semantic refresh без cloud API используй только `scripts/graphify_local_extract.sh`; он запускает `graphify extract` через локальный Ollama, сам управляет временным Ollama-процессом и может быть медленным.
- Не запускай raw `graphify extract` и cloud-backed `graphify label` в этом репозитории без прямого запроса пользователя. Внешняя интеграция Graphify нужна только для semantic extraction: она читает документацию/config/chunks и добавляет LLM-связи, wiki и человекочитаемые labels. Рабочий режим проекта — локальный AST-граф + локальная кластеризация; labels/semantic refresh — опционально через локальную модель.
- `.graphifyignore` исключает CI workflows, generated Swagger, Grafana dashboards, build output и IDE metadata из графа. При изменении CI/Docker/deployment файлов проверяй актуальные файлы напрямую через `rg`.
- Не редактируй содержимое `graphify-out/` вручную и не добавляй его в Git.
