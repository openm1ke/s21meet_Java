#!/usr/bin/env bash
set -euo pipefail

# ---------- config ----------
APP_SERVICES=(s21auth s21edu s21bot s21rocket)
BUILDABLE_IMAGES=(s21auth s21edu s21edu-migrator s21bot s21rocket)
INFRA_SERVICES=(postgres prometheus grafana)
TEST_ENV_DIR="env/test"
COMPOSE_ENV_FILE="$TEST_ENV_DIR/compose.env"

# prefer docker compose v2, fallback to docker-compose
if command -v docker &>/dev/null && docker compose version &>/dev/null; then
  COMPOSE=(docker compose)
elif command -v docker-compose &>/dev/null; then
  COMPOSE=(docker-compose)
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
  [[ -f docker-compose.yml ]]
}

copy_if_missing() {
  local src="$1"
  local dst="$2"

  if [[ -f "$dst" ]]; then
    return 0
  fi

  mkdir -p "$(dirname "$dst")"
  cp "$src" "$dst"
  info "Created $dst from $(basename "$src")"
}

normalize_test_env() {
  perl -0pi -e 's|^DB_URL=.*$|DB_URL=jdbc:postgresql://postgres:5432/postgres|m; s|^TOKEN_ENDPOINT=.*$|TOKEN_ENDPOINT=http://s21auth:8081/api/tokens/default|m; s|^BOT_SERVICE_URL=.*$|BOT_SERVICE_URL=http://s21bot:8083|m' "$TEST_ENV_DIR/s21edu.env"
  perl -0pi -e 's|^PROFILE_SERVICE_URL=.*$|PROFILE_SERVICE_URL=http://s21edu:8082|m; s|^ROCKETCHAT_SERVICE_URL=.*$|ROCKETCHAT_SERVICE_URL=http://s21rocket:8084|m' "$TEST_ENV_DIR/s21bot.env"
}

ensure_test_env() {
  copy_if_missing "$TEST_ENV_DIR/compose.env.example" "$COMPOSE_ENV_FILE"
  copy_if_missing "$TEST_ENV_DIR/postgres.env.example" "$TEST_ENV_DIR/postgres.env"

  if [[ -f s21auth/.env ]]; then
    copy_if_missing "s21auth/.env" "$TEST_ENV_DIR/s21auth.env"
  else
    copy_if_missing "$TEST_ENV_DIR/s21auth.env.example" "$TEST_ENV_DIR/s21auth.env"
  fi

  if [[ -f s21edu/.env ]]; then
    copy_if_missing "s21edu/.env" "$TEST_ENV_DIR/s21edu.env"
  else
    copy_if_missing "$TEST_ENV_DIR/s21edu.env.example" "$TEST_ENV_DIR/s21edu.env"
  fi

  if [[ -f s21bot/.env ]]; then
    copy_if_missing "s21bot/.env" "$TEST_ENV_DIR/s21bot.env"
  else
    copy_if_missing "$TEST_ENV_DIR/s21bot.env.example" "$TEST_ENV_DIR/s21bot.env"
  fi

  if [[ -f s21rocket/.env ]]; then
    copy_if_missing "s21rocket/.env" "$TEST_ENV_DIR/s21rocket.env"
  else
    copy_if_missing "$TEST_ENV_DIR/s21rocket.env.example" "$TEST_ENV_DIR/s21rocket.env"
  fi

  normalize_test_env
}

compose_cmd() {
  "${COMPOSE[@]}" --profile infra --env-file "$COMPOSE_ENV_FILE" -f docker-compose.yml "$@"
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

resolve_images_for_targets() {
  local requested=("$@")
  local images=()

  for s in "${requested[@]}"; do
    images+=("$s")
    if [[ "$s" == "s21edu" ]]; then
      images+=("s21edu-migrator")
    fi
  done

  printf '%s\n' "${images[@]}" | awk '!seen[$0]++'
}

build_images() {
  local images=("$@")
  local image_repo image_tag

  image_repo="$(grep '^IMAGE_REPO=' "$COMPOSE_ENV_FILE" | cut -d= -f2-)"
  image_tag="$(grep '^IMAGE_TAG=' "$COMPOSE_ENV_FILE" | cut -d= -f2-)"

  for image in "${images[@]}"; do
    case "$image" in
      s21auth|s21edu|s21bot|s21rocket)
        info "Docker build: $image"
        docker build -t "${image_repo}/${image}:${image_tag}" "./${image}"
        ;;
      s21edu-migrator)
        info "Docker build: $image"
        docker build -f s21edu/migrator.Dockerfile -t "${image_repo}/${image}:${image_tag}" ./s21edu
        ;;
      *)
        err "Unsupported image target: $image"
        exit 1
        ;;
    esac
  done
}

compose_up_targets() {
  local services=("$@")

  local extra=()
  [[ "$RECREATE" -eq 1 ]] && extra+=(--force-recreate)

  # по умолчанию не трогаем зависимости, чтобы не перезапускать весь зоопарк
  if [[ "$WITH_DEPS" -eq 0 ]]; then
    extra+=(--no-deps)
  fi

  info "Compose up targets: ${services[*]}"
  compose_cmd up -d "${extra[@]}" "${services[@]}"
}

compose_restart_targets() {
  local services=("$@")
  info "Compose restart targets: ${services[*]}"
  compose_cmd restart "${services[@]}"
}

wait_for_postgres() {
  local retries=30
  local delay=2

  info "Waiting for postgres to become healthy..."
  for _ in $(seq 1 "$retries"); do
    if compose_cmd ps --format json postgres 2>/dev/null | rg -q '"Health":"healthy"'; then
      return 0
    fi
    sleep "$delay"
  done

  err "Postgres did not become healthy in time"
  exit 1
}

# ---------- main ----------
if ! compose_file_ok; then
  err "Compose file not found in current directory!"
  exit 1
fi

ensure_test_env

if [[ "$DO_DOWN" -eq 1 ]]; then
  info "Stopping stack..."
  compose_cmd down --remove-orphans || true
  exit 0
fi

if [[ "$FULL" -eq 1 ]]; then
  info "FULL mode: down + build ALL + up ALL (slow)"
  compose_cmd down --remove-orphans || true

  if [[ "$NO_BUILD" -eq 0 ]]; then
    info "Gradle build ALL app services (no tests)"
    if [[ "$CLEAN" -eq 1 ]]; then
      ./gradlew clean -x test --parallel build
    else
      ./gradlew -x test --parallel build
    fi
    build_images "${BUILDABLE_IMAGES[@]}"
  fi

  info "Compose up all services..."
  compose_cmd up -d
  [[ "$SHOW_PS" -eq 1 ]] && compose_cmd ps
  info "✅ Done"
  exit 0
fi

# Ensure infra is up (быстро, если уже поднято)
info "Ensuring infra is up: ${INFRA_SERVICES[*]}"
compose_cmd up -d "${INFRA_SERVICES[@]}"
wait_for_postgres

if [[ "$RESTART_ONLY" -eq 1 ]]; then
  compose_restart_targets "${TARGETS[@]}"
  [[ "$SHOW_PS" -eq 1 ]] && compose_cmd ps
  info "✅ Done"
  exit 0
fi

if [[ "$NO_BUILD" -eq 0 ]]; then
  info "Building jars only for: ${TARGETS[*]}"
  gradle_build "${TARGETS[@]}"
  IMAGE_TARGETS=()
  while IFS= read -r image; do
    [[ -n "$image" ]] && IMAGE_TARGETS+=("$image")
  done < <(resolve_images_for_targets "${TARGETS[@]}")
  build_images "${IMAGE_TARGETS[@]}"
else
  warn "Skipping Gradle build (--no-build)"
fi

compose_up_targets "${TARGETS[@]}"

[[ "$SHOW_PS" -eq 1 ]] && compose_cmd ps
info "✅ Done"
