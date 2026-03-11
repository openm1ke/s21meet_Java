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

case "$STRATEGY" in
  rolling)
    "${compose_cmd[@]}" pull
    "${compose_cmd[@]}" run --rm s21edu-migrator
    "${compose_cmd[@]}" up -d --remove-orphans
    ;;
  recreate-all)
    "${compose_cmd[@]}" pull
    "${compose_cmd[@]}" down --remove-orphans
    # Bring infra back before migrations for test profile deployments.
    if [[ -n "$PROFILE_NAME" ]]; then
      "${compose_cmd[@]}" up -d postgres prometheus grafana
    fi
    "${compose_cmd[@]}" run --rm s21edu-migrator
    "${compose_cmd[@]}" up -d --force-recreate --remove-orphans
    ;;
  *)
    echo "Unknown strategy: $STRATEGY"
    exit 1
    ;;
esac
