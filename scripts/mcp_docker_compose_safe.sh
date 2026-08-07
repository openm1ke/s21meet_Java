#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
SOCKET="${DOCKER_MCP_SOCKET:-$HOME/.docker/run/docker.sock}"
IMAGE="${DOCKER_MCP_IMAGE:-ghcr.io/l337-org/docker-mcp-server:no-scout}"

if [[ ! -S "$SOCKET" ]]; then
  echo "Docker socket not found: $SOCKET" >&2
  echo "Start Docker Desktop or set DOCKER_MCP_SOCKET." >&2
  exit 1
fi

exec docker run --rm -i \
  -v "$SOCKET:/var/run/docker.sock" \
  -v "$ROOT_DIR:$ROOT_DIR" \
  -w "$ROOT_DIR" \
  -e DOCKER_MCP_SERVER_NO_DESTRUCTIVE=1 \
  -e DOCKER_MCP_SERVER_DISABLE="${DOCKER_MCP_SERVER_DISABLE:-swarm,stack,scout,hub,registry}" \
  "$IMAGE"
