#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

cd "$ROOT_DIR"

# Keep routine graph refresh local even when cloud LLM keys exist in the shell.
unset GEMINI_API_KEY GOOGLE_API_KEY
unset OPENAI_API_KEY OPENAI_BASE_URL OPENAI_MODEL
unset ANTHROPIC_API_KEY ANTHROPIC_BASE_URL ANTHROPIC_MODEL
unset DEEPSEEK_API_KEY KIMI_API_KEY

graphify update .
graphify cluster-only . --no-label
