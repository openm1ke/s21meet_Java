#!/usr/bin/env bash
set -euo pipefail

# ---------- config ----------
APP_SERVICES=(s21auth s21edu s21bot s21rocket)
INFRA_SERVICES=(postgres prometheus grafana)

# prefer docker compose v2, fallback to docker-compose
if command -v docker &>/dev/null && docker compose version &>/dev/null; then
  COMPOSE="docker compose"
elif command -v docker-compose &>/dev/null; then
  COMPOSE="docker-compose"
else
  echo "[ERROR] docker compose / docker-compose not found"
  exit 1
fi

# ---------- colors/log ----------
GREEN='\033[0;32m'; YELLOW='\033[1;33m'; RED='\033[0;31m'; NC='\033[0m'
info(){ echo -e "${GREEN}[INFO]${NC} $*"; }
warn(){ echo -e "${YELLOW}[WARN]${NC} $*"; }
err(){  echo -e "${RED}[ERROR]${NC} $*"; }

usage() {
  cat <<'USAGE'
Usage:
  ./dev.sh                 # build+up all app services (infra kept)
  ./dev.sh all             # same as above
  ./dev.sh infra           # start infra only
  ./dev.sh s21edu          # build+up only s21edu
  ./dev.sh s21edu s21bot   # build+up only listed services

Options:
  --full        # like old behavior: down + build all + up all (slow but clean)
  --clean       # gradle clean for selected modules before build
  --no-build    # skip gradle; only docker compose up/recreate
  --restart     # restart containers only (no build, no docker build)
  --recreate    # add --force-recreate to compose up
  --with-deps   # do not use --no-deps (recreate deps too)
  --down        # stop stack (down)
  --ps          # show compose ps

Examples:
  ./dev.sh s21edu
  ./dev.sh s21edu s21bot --recreate
  ./dev.sh infra
  ./dev.sh --full
USAGE
}

# ---------- args parsing ----------
FULL=0
CLEAN=0
NO_BUILD=0
RESTART_ONLY=0
RECREATE=0
WITH_DEPS=0
DO_DOWN=0
SHOW_PS=0

TARGETS=()

while [[ $# -gt 0 ]]; do
  case "$1" in
    -h|--help) usage; exit 0 ;;
    --full) FULL=1; shift ;;
    --clean) CLEAN=1; shift ;;
    --no-build) NO_BUILD=1; shift ;;
    --restart) RESTART_ONLY=1; NO_BUILD=1; shift ;;
    --recreate) RECREATE=1; shift ;;
    --with-deps) WITH_DEPS=1; shift ;;
    --down) DO_DOWN=1; shift ;;
    --ps) SHOW_PS=1; shift ;;
    all) TARGETS=("${APP_SERVICES[@]}"); shift ;;
    infra) TARGETS=("${INFRA_SERVICES[@]}"); shift ;;
    *) TARGETS+=("$1"); shift ;;
  esac
done

# default targets: all app services
if [[ ${#TARGETS[@]} -eq 0 ]]; then
  TARGETS=("${APP_SERVICES[@]}")
fi

# ---------- helpers ----------
compose_file_ok() {
  [[ -f docker-compose.yml || -f compose.yml || -f docker-compose.yaml || -f compose.yaml ]]
}

gradle_build() {
  local services=("$@")
  local tasks=()

  for s in "${services[@]}"; do
    # предполагаем, что gradle subproject = service name
    if [[ "$CLEAN" -eq 1 ]]; then
      tasks+=(":${s}:clean")
    fi
    # сначала bootJar (spring boot), если вдруг не spring-boot — сменишь на :$s:build
    tasks+=(":${s}:bootJar")
  done

  info "Gradle tasks: ${tasks[*]}"
  # --parallel ускоряет multi-module
  ./gradlew -x test --parallel "${tasks[@]}"
}

compose_up_targets() {
  local services=("$@")

  local extra=()
  [[ "$RECREATE" -eq 1 ]] && extra+=(--force-recreate)

  # по умолчанию не трогаем зависимости, чтобы не перезапускать весь зоопарк
  if [[ "$WITH_DEPS" -eq 0 ]]; then
    extra+=(--no-deps)
  fi

  info "Compose up (build) targets: ${services[*]}"
  $COMPOSE up -d --build "${extra[@]}" "${services[@]}"
}

compose_restart_targets() {
  local services=("$@")
  info "Compose restart targets: ${services[*]}"
  $COMPOSE restart "${services[@]}"
}

# ---------- main ----------
if ! compose_file_ok; then
  err "Compose file not found in current directory!"
  exit 1
fi

if [[ "$DO_DOWN" -eq 1 ]]; then
  info "Stopping stack..."
  $COMPOSE down --remove-orphans || true
  exit 0
fi

if [[ "$FULL" -eq 1 ]]; then
  info "FULL mode: down + build ALL + up ALL (slow)"
  $COMPOSE down --remove-orphans || true

  if [[ "$NO_BUILD" -eq 0 ]]; then
    info "Gradle build ALL app services (no tests)"
    if [[ "$CLEAN" -eq 1 ]]; then
      ./gradlew clean -x test --parallel build
    else
      ./gradlew -x test --parallel build
    fi
  fi

  info "Compose up all services..."
  $COMPOSE up -d --build
  [[ "$SHOW_PS" -eq 1 ]] && $COMPOSE ps
  info "✅ Done"
  exit 0
fi

# Ensure infra is up (быстро, если уже поднято)
info "Ensuring infra is up: ${INFRA_SERVICES[*]}"
$COMPOSE up -d "${INFRA_SERVICES[@]}"

if [[ "$RESTART_ONLY" -eq 1 ]]; then
  compose_restart_targets "${TARGETS[@]}"
  [[ "$SHOW_PS" -eq 1 ]] && $COMPOSE ps
  info "✅ Done"
  exit 0
fi

if [[ "$NO_BUILD" -eq 0 ]]; then
  info "Building jars only for: ${TARGETS[*]}"
  gradle_build "${TARGETS[@]}"
else
  warn "Skipping Gradle build (--no-build)"
fi

compose_up_targets "${TARGETS[@]}"

[[ "$SHOW_PS" -eq 1 ]] && $COMPOSE ps
info "✅ Done"