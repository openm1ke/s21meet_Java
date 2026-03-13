#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 1 || $# -gt 3 ]]; then
  echo "Usage: $0 <compose-env-file> [profile] [strategy]"
  echo "Strategies:"
  echo "  rolling      - default, update running app stack"
  echo "  recreate-all - recreate all containers (for test env)"
  exit 1
fi

COMPOSE_ENV_FILE="$1"
PROFILE_NAME="${2:-}"
STRATEGY="${3:-rolling}"
COMPOSE_FILE="docker-compose.yml"

if [[ ! -f "$COMPOSE_ENV_FILE" ]]; then
  echo "Compose env file not found: $COMPOSE_ENV_FILE"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_FILE"
  exit 1
fi

compose_cmd=(docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
if [[ -n "$PROFILE_NAME" ]]; then
  compose_cmd=(docker compose --profile "$PROFILE_NAME" --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
fi

wait_for_postgres_healthy() {
  local timeout_seconds="${1:-180}"
  local poll_interval=3
  local waited=0
  local container_id status

  container_id="$("${compose_cmd[@]}" ps -q postgres 2>/dev/null || true)"
  if [[ -z "$container_id" ]]; then
    echo "Postgres container not found, skip readiness wait"
    return 0
  fi

  while (( waited < timeout_seconds )); do
    status="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' "$container_id" 2>/dev/null || true)"
    if [[ "$status" == "healthy" ]]; then
      echo "Postgres is healthy"
      return 0
    fi
    sleep "$poll_interval"
    waited=$((waited + poll_interval))
  done

  echo "Timeout waiting for postgres to become healthy"
  return 1
}

case "$STRATEGY" in
  rolling)
    app_services=(s21auth s21edu s21rocket s21bot)
    "${compose_cmd[@]}" pull
    if [[ -n "$PROFILE_NAME" ]]; then
      "${compose_cmd[@]}" up -d postgres
      wait_for_postgres_healthy 180
    fi
    "${compose_cmd[@]}" run --rm s21edu-migrator
    "${compose_cmd[@]}" up -d --remove-orphans "${app_services[@]}"
    ;;
  recreate-all)
    app_services=(s21auth s21edu s21rocket s21bot)
    "${compose_cmd[@]}" pull
    "${compose_cmd[@]}" down --remove-orphans
    # Bring infra back before migrations for test profile deployments.
    if [[ -n "$PROFILE_NAME" ]]; then
      "${compose_cmd[@]}" up -d postgres prometheus grafana
      wait_for_postgres_healthy 180
    fi
    "${compose_cmd[@]}" run --rm s21edu-migrator
    "${compose_cmd[@]}" up -d --force-recreate --remove-orphans "${app_services[@]}"
    ;;
  *)
    echo "Unknown strategy: $STRATEGY"
    exit 1
    ;;
esac
