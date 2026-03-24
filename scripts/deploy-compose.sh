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
PROXY_MODE=""
PROXY_SERVICE=""
INACTIVE_PROXY_SERVICES=()

if [[ ! -f "$COMPOSE_ENV_FILE" ]]; then
  echo "Compose env file not found: $COMPOSE_ENV_FILE"
  exit 1
fi

if [[ ! -f "$COMPOSE_FILE" ]]; then
  echo "Compose file not found: $COMPOSE_FILE"
  exit 1
fi

PROXY_MODE="$(grep '^PROXY_MODE=' "$COMPOSE_ENV_FILE" | cut -d= -f2- || true)"

compose_cmd=(docker compose --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
if [[ -n "$PROFILE_NAME" ]]; then
  compose_cmd=(docker compose --profile "$PROFILE_NAME" --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE")
fi

case "${PROXY_MODE:-vless}" in
  vless)
    PROXY_SERVICE="xray-client"
    INACTIVE_PROXY_SERVICES=("ssh-socks-client")
    compose_cmd=(docker compose --profile proxy-vless "${compose_cmd[@]:2}")
    ;;
  ssh)
    PROXY_SERVICE="ssh-socks-client"
    INACTIVE_PROXY_SERVICES=("xray-client")
    compose_cmd=(docker compose --profile proxy-ssh "${compose_cmd[@]:2}")
    ;;
  none)
    PROXY_SERVICE=""
    INACTIVE_PROXY_SERVICES=("xray-client" "ssh-socks-client")
    ;;
  *)
    echo "Unsupported PROXY_MODE: $PROXY_MODE (allowed: vless|ssh|none)"
    exit 1
    ;;
esac

remove_inactive_proxies() {
  local service container_id
  for service in "${INACTIVE_PROXY_SERVICES[@]}"; do
    case "$service" in
      xray-client)
        container_id="$(docker compose --profile proxy-vless --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" ps -q "$service" 2>/dev/null || true)"
        ;;
      ssh-socks-client)
        container_id="$(docker compose --profile proxy-ssh --env-file "$COMPOSE_ENV_FILE" -f "$COMPOSE_FILE" ps -q "$service" 2>/dev/null || true)"
        ;;
      *)
        container_id=""
        ;;
    esac

    if [[ -n "$container_id" ]]; then
      echo "Removing inactive proxy container for service: $service"
      docker rm -f "$container_id" >/dev/null 2>&1 || true
    fi
  done
  return 0
}

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
    if [[ -n "$PROXY_SERVICE" ]]; then
      app_services+=("$PROXY_SERVICE")
    fi
    "${compose_cmd[@]}" pull
    remove_inactive_proxies
    if [[ -n "$PROFILE_NAME" ]]; then
      "${compose_cmd[@]}" up -d postgres
      wait_for_postgres_healthy 180
    fi
    "${compose_cmd[@]}" run --rm s21edu-migrator
    "${compose_cmd[@]}" up -d --remove-orphans "${app_services[@]}"
    ;;
  recreate-all)
    app_services=(s21auth s21edu s21rocket s21bot)
    if [[ -n "$PROXY_SERVICE" ]]; then
      app_services+=("$PROXY_SERVICE")
    fi
    "${compose_cmd[@]}" pull
    remove_inactive_proxies
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
