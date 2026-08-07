#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ENV="${APP_ENV:-test}"
ENV_FILE="$ROOT_DIR/env/$APP_ENV/postgres.env"

cd "$ROOT_DIR"

if [[ -n "${PG_MCP_DATABASE_URL:-}" ]]; then
  exec pipx run postgres-mcp-server "$PG_MCP_DATABASE_URL"
fi

if [[ ! -f "$ENV_FILE" ]]; then
  echo "Postgres env file not found: $ENV_FILE" >&2
  echo "Set PG_MCP_DATABASE_URL or create env/$APP_ENV/postgres.env." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a

POSTGRES_HOST="${POSTGRES_HOST:-127.0.0.1}"
POSTGRES_PORT="${POSTGRES_PORT:-5432}"

if [[ -z "${POSTGRES_DB:-}" || -z "${POSTGRES_USER:-}" ]]; then
  echo "POSTGRES_DB and POSTGRES_USER must be set in $ENV_FILE." >&2
  exit 1
fi

DATABASE_URL="postgresql://${POSTGRES_USER}:${POSTGRES_PASSWORD:-}@${POSTGRES_HOST}:${POSTGRES_PORT}/${POSTGRES_DB}"
exec pipx run postgres-mcp-server "$DATABASE_URL"
