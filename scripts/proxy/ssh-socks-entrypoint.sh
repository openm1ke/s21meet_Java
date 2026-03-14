#!/bin/sh
set -eu

FR_HOST="${FR_HOST:?FR_HOST is required}"
FR_PORT="${FR_PORT:-22}"
FR_USER="${FR_USER:?FR_USER is required}"
TUNNEL_SOCKS_PORT="${TUNNEL_SOCKS_PORT:-1080}"
SSH_KEY_PATH="${SSH_KEY_PATH:-/run/secrets/ssh_private_key}"
SSH_KNOWN_HOSTS_PATH="${SSH_KNOWN_HOSTS_PATH:-/run/secrets/ssh_known_hosts}"

if [ ! -r "$SSH_KEY_PATH" ]; then
  echo "ERROR: SSH private key not found or not readable: $SSH_KEY_PATH"
  exit 1
fi

chmod 600 "$SSH_KEY_PATH" 2>/dev/null || true

if [ ! -s "$SSH_KNOWN_HOSTS_PATH" ]; then
  echo "ERROR: known_hosts is missing or empty: $SSH_KNOWN_HOSTS_PATH"
  exit 1
fi

HOSTPORT="[$FR_HOST]:$FR_PORT"
if ! ssh-keygen -F "$HOSTPORT" -f "$SSH_KNOWN_HOSTS_PATH" >/dev/null 2>&1; then
  echo "ERROR: known_hosts does not contain entry for $HOSTPORT"
  exit 1
fi

echo "Starting SSH SOCKS tunnel to $FR_USER@$FR_HOST:$FR_PORT on 0.0.0.0:$TUNNEL_SOCKS_PORT"

exec autossh -M 0 -NT \
  -o ExitOnForwardFailure=yes \
  -o ServerAliveInterval=15 \
  -o ServerAliveCountMax=3 \
  -o ConnectTimeout=5 \
  -o IdentitiesOnly=yes \
  -o PasswordAuthentication=no \
  -o BatchMode=yes \
  -o StrictHostKeyChecking=yes \
  -o UserKnownHostsFile="$SSH_KNOWN_HOSTS_PATH" \
  -i "$SSH_KEY_PATH" \
  -p "$FR_PORT" \
  -g -D 0.0.0.0:"$TUNNEL_SOCKS_PORT" \
  "${FR_USER}@${FR_HOST}"
