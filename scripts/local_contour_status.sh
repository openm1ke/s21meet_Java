#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
APP_ENV="${APP_ENV:-test}"

cd "$ROOT_DIR"

echo "Docker Compose status (APP_ENV=$APP_ENV):"
APP_ENV="$APP_ENV" docker compose ps

echo
echo "Actuator health:"
check_health() {
  local name="$1"
  local url="$2"

  if curl -fsS --max-time 2 "$url" >/tmp/s21meet-health.json 2>/dev/null; then
    printf '  %-10s OK    %s\n' "$name" "$url"
  else
    printf '  %-10s FAIL  %s\n' "$name" "$url"
  fi
}

check_health s21auth "http://127.0.0.1:8081/actuator/health"
check_health s21edu "http://127.0.0.1:8082/actuator/health"
check_health s21bot "http://127.0.0.1:8083/actuator/health"
check_health s21rocket "http://127.0.0.1:8084/actuator/health"
check_health s21web "http://127.0.0.1:8085/actuator/health"

rm -f /tmp/s21meet-health.json
