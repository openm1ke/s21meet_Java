#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
MODEL="${GRAPHIFY_LOCAL_EXTRACT_MODEL:-graphify-qwen2.5-coder:7b}"
TOKEN_BUDGET="${GRAPHIFY_LOCAL_EXTRACT_TOKEN_BUDGET:-1500}"
OLLAMA_URL="${OLLAMA_HOST:-http://127.0.0.1:11434}"
OLLAMA_PID=""
DEFAULT_MODEL="graphify-qwen2.5-coder:7b"
BASE_MODEL="qwen2.5-coder:7b"

cd "$ROOT_DIR"

if ! command -v ollama >/dev/null 2>&1; then
  cat >&2 <<'EOF'
Ollama is not installed, so local Graphify semantic extraction cannot run yet.

Install once:
  brew install ollama
  ollama serve
  ollama pull qwen2.5-coder:7b
  ollama create graphify-qwen2.5-coder:7b -f /private/tmp/graphify-qwen2.5-coder.Modelfile

Then rerun:
  scripts/graphify_local_extract.sh

To use another local model:
  GRAPHIFY_LOCAL_EXTRACT_MODEL=<model> scripts/graphify_local_extract.sh

To tune local chunk size:
  GRAPHIFY_LOCAL_EXTRACT_TOKEN_BUDGET=1500 scripts/graphify_local_extract.sh
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
  OLLAMA_HOST="$OLLAMA_URL" ollama serve >/tmp/graphify-ollama-extract.log 2>&1 &
  OLLAMA_PID="$!"

  for _ in $(seq 1 60); do
    if ollama_is_ready; then
      echo "Ollama is ready."
      return
    fi
    sleep 1
  done

  echo "Ollama did not become ready in time. Log:" >&2
  tail -80 /tmp/graphify-ollama-extract.log >&2 || true
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

ensure_model() {
  if ollama list | awk 'NR > 1 { print $1 }' | grep -Fxq "$MODEL"; then
    return
  fi

  if [[ "$MODEL" != "$DEFAULT_MODEL" ]]; then
    echo "Ollama model \"$MODEL\" is not available locally. Pulling it now..."
    ollama pull "$MODEL"
    return
  fi

  if ! ollama list | awk 'NR > 1 { print $1 }' | grep -Fxq "$BASE_MODEL"; then
    echo "Base Ollama model \"$BASE_MODEL\" is not available locally. Pulling it now..."
    ollama pull "$BASE_MODEL"
  fi

  local modelfile
  modelfile="$(mktemp "${TMPDIR:-/tmp}/graphify-qwen2.5-coder.XXXXXX.Modelfile")"
  cat >"$modelfile" <<'EOF'
FROM qwen2.5-coder:7b
PARAMETER temperature 0
PARAMETER top_p 0.1
PARAMETER num_ctx 8192
SYSTEM """
You are a strict JSON generation engine for Graphify.
Return only the exact JSON object or JSON array requested by the user.
Do not write explanations, Markdown, code fences, comments, or prose.
If information is missing, return an empty valid JSON structure that matches the requested schema.
"""
EOF

  echo "Creating local Ollama model \"$DEFAULT_MODEL\" for strict Graphify JSON responses..."
  ollama create "$DEFAULT_MODEL" -f "$modelfile"
  rm -f "$modelfile"
}

ensure_model

# Force local extraction. Do not let Graphify auto-detect cloud keys from the shell.
unset GEMINI_API_KEY GOOGLE_API_KEY
unset OPENAI_API_KEY OPENAI_BASE_URL OPENAI_MODEL
unset ANTHROPIC_API_KEY ANTHROPIC_BASE_URL ANTHROPIC_MODEL
unset DEEPSEEK_API_KEY KIMI_API_KEY

OLLAMA_API_KEY="${OLLAMA_API_KEY:-local}" OLLAMA_HOST="$OLLAMA_URL" \
  graphify extract . \
    --backend=ollama \
    --model="$MODEL" \
    --max-concurrency=1 \
    --token-budget="$TOKEN_BUDGET" \
    "$@"
