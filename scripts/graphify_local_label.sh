#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL="${GRAPHIFY_LOCAL_LABEL_MODEL:-qwen2.5-coder:7b}"
OLLAMA_URL="${OLLAMA_HOST:-http://127.0.0.1:11434}"
OLLAMA_PID=""
LABEL_MISSING_ONLY="${GRAPHIFY_LABEL_MISSING_ONLY:-0}"

cd "$ROOT_DIR"

if ! command -v ollama >/dev/null 2>&1; then
  cat >&2 <<'EOF'
Ollama is not installed, so local Graphify labels cannot be generated yet.

Install once:
  brew install ollama
  ollama serve
  ollama pull qwen2.5-coder:7b

Then rerun:
  scripts/graphify_local_label.sh

To use another local model:
  GRAPHIFY_LOCAL_LABEL_MODEL=<model> scripts/graphify_local_label.sh
EOF
  exit 1
fi

ollama_is_ready() {
  curl -fsS "$OLLAMA_URL/api/tags" >/dev/null 2>&1
}

start_ollama_if_needed() {
  if ollama_is_ready; then
    echo "Ollama is already running at $OLLAMA_URL"
    return
  fi

  echo "Starting temporary Ollama server at $OLLAMA_URL..."
  OLLAMA_HOST="$OLLAMA_URL" ollama serve >/tmp/graphify-ollama-label.log 2>&1 &
  OLLAMA_PID="$!"

  for _ in $(seq 1 60); do
    if ollama_is_ready; then
      echo "Ollama is ready."
      return
    fi
    sleep 1
  done

  echo "Ollama did not become ready in time. Log:" >&2
  tail -80 /tmp/graphify-ollama-label.log >&2 || true
  exit 1
}

stop_ollama_if_started() {
  if [[ -n "$OLLAMA_PID" ]]; then
    echo "Stopping temporary Ollama server..."
    kill "$OLLAMA_PID" >/dev/null 2>&1 || true
    wait "$OLLAMA_PID" >/dev/null 2>&1 || true
  fi
}

trap stop_ollama_if_started EXIT
start_ollama_if_needed

if ! ollama list | awk 'NR > 1 { print $1 }' | grep -Fxq "$MODEL"; then
  echo "Ollama model \"$MODEL\" is not available locally. Pulling it now..."
  ollama pull "$MODEL"
fi

# Force local naming. Do not let Graphify auto-detect cloud keys from the shell.
unset GEMINI_API_KEY GOOGLE_API_KEY
unset OPENAI_API_KEY OPENAI_BASE_URL OPENAI_MODEL
unset ANTHROPIC_API_KEY ANTHROPIC_BASE_URL ANTHROPIC_MODEL
unset DEEPSEEK_API_KEY KIMI_API_KEY

if [[ "$LABEL_MISSING_ONLY" == "1" ]]; then
  OLLAMA_API_KEY="${OLLAMA_API_KEY:-local}" OLLAMA_HOST="$OLLAMA_URL" \
    graphify label . --backend=ollama --model="$MODEL" --max-concurrency=1 --missing-only
else
  OLLAMA_API_KEY="${OLLAMA_API_KEY:-local}" OLLAMA_HOST="$OLLAMA_URL" \
    graphify label . --backend=ollama --model="$MODEL" --max-concurrency=1
fi
