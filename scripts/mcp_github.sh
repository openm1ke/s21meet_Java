#!/usr/bin/env bash
set -euo pipefail

TOKEN="${GITHUB_PERSONAL_ACCESS_TOKEN:-${GITHUB_PAT:-}}"
TOOLSETS="${GITHUB_TOOLSETS:-context,repos,issues,pull_requests,actions,code_security,dependabot}"

if [[ -n "$TOKEN" ]]; then
  exec docker run -i --rm \
    -e GITHUB_PERSONAL_ACCESS_TOKEN \
    -e GITHUB_TOOLSETS \
    -e GITHUB_HOST \
    -e GITHUB_READ_ONLY \
    -e GITHUB_DYNAMIC_TOOLSETS \
    ghcr.io/github/github-mcp-server
fi

exec docker run -i --rm \
  -p 127.0.0.1:8085:8085 \
  -e GITHUB_OAUTH_CALLBACK_PORT=8085 \
  -e GITHUB_TOOLSETS="$TOOLSETS" \
  ghcr.io/github/github-mcp-server
