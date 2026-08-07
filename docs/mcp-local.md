# Local MCP Workflow

Project MCP configuration lives in `.mcp.json`. It is intentionally secret-free: tokens and database credentials are read from local environment variables or ignored `env/<profile>/*.env` files.

## Servers

| Server | Purpose | Notes |
| --- | --- | --- |
| `graphify` | Code graph navigation and impact analysis | Uses local `graphify-out/graph.json`. |
| `github` | PRs, issues, Actions, repo context | Uses official `ghcr.io/github/github-mcp-server`. Set `GITHUB_PERSONAL_ACCESS_TOKEN` or `GITHUB_PAT`, otherwise OAuth callback on `127.0.0.1:8085` is used. |
| `postgres-test` | Read-only SQL/schema access to the test DB | Uses `postgres-mcp-server`; loads `env/${APP_ENV:-test}/postgres.env`; override with `PG_MCP_DATABASE_URL`. |
| `docker-readonly` | Inspect containers, logs, stats, images, compose state | Read-only mode. Requires Docker Desktop. |
| `docker-compose-safe` | Compose-oriented local test-contour control | No destructive tools. Mounts this repo into the MCP container so compose paths resolve. |
| `playwright` | Browser checks for `s21web` | Headless, isolated browser; output goes to `.mcp-output/playwright`. |

## Environment

GitHub:

```bash
export GITHUB_PAT=<fine-grained-token>
export GITHUB_TOOLSETS=context,repos,issues,pull_requests,actions,code_security,dependabot
```

Postgres:

```bash
APP_ENV=test scripts/mcp_postgres_test.sh
```

Or bypass project env files:

```bash
export PG_MCP_DATABASE_URL='postgresql://postgres:change-me@127.0.0.1:5432/postgres'
```

Docker on macOS:

```bash
open -a Docker
scripts/mcp_docker_readonly.sh
scripts/mcp_docker_compose_safe.sh
```

If Docker Desktop exposes a different socket:

```bash
export DOCKER_MCP_SOCKET=/var/run/docker.sock
```

## Local Contour Checks

Start the test contour with existing project commands, then agents can inspect it through Docker/Postgres/Playwright MCP:

```bash
APP_ENV=test docker compose --profile infra up -d postgres prometheus grafana
APP_ENV=test docker compose ps
```

Health endpoints:

```bash
curl -fsS http://127.0.0.1:8081/actuator/health
curl -fsS http://127.0.0.1:8082/actuator/health
curl -fsS http://127.0.0.1:8083/actuator/health
curl -fsS http://127.0.0.1:8084/actuator/health
curl -fsS http://127.0.0.1:8085/actuator/health
```

## Quality Utilities

Recommended local tools:

```bash
brew install actionlint hadolint gitleaks
```

Useful checks:

```bash
actionlint
hadolint */Dockerfile
gitleaks detect --source . --redact
./gradlew check
```

Project wrappers:

```bash
scripts/local_contour_status.sh
scripts/quality_local.sh fast
scripts/quality_local.sh
```
