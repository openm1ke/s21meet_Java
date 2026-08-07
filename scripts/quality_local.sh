#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODE="${1:-full}"

cd "$ROOT_DIR"

run_if_available() {
  local tool="$1"
  shift

  if command -v "$tool" >/dev/null 2>&1; then
    "$tool" "$@"
  else
    echo "skip: $tool is not installed" >&2
  fi
}

echo "Checking shell scripts..."
bash -n scripts/*.sh

echo "Checking MCP config..."
python3 -m json.tool .mcp.json >/dev/null

echo "Checking GitHub Actions workflows..."
run_if_available actionlint

echo "Checking Dockerfiles..."
if command -v hadolint >/dev/null 2>&1; then
  find . -path './.git' -prune -o -name 'Dockerfile*' -type f -print0 | xargs -0 hadolint
else
  echo "skip: hadolint is not installed" >&2
fi

echo "Checking secrets..."
run_if_available gitleaks detect --source . --redact

if [[ "$MODE" == "fast" ]]; then
  echo "Fast checks complete."
  exit 0
fi

echo "Running Gradle check..."
./gradlew check
