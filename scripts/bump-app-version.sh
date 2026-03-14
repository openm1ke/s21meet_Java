#!/usr/bin/env bash
set -euo pipefail

FILE_PATH="${1:-gradle.properties}"
PROPERTY_NAME="${2:-appVersion}"

if [[ ! -f "$FILE_PATH" ]]; then
  echo "File not found: $FILE_PATH" >&2
  exit 1
fi

current_version="$(grep -E "^${PROPERTY_NAME}=" "$FILE_PATH" | head -n1 | cut -d'=' -f2- | sed -E 's/\r$//; s/^[[:space:]]+//; s/[[:space:]]+$//' || true)"
if [[ -z "$current_version" ]]; then
  echo "Property '${PROPERTY_NAME}' not found in $FILE_PATH" >&2
  exit 1
fi

if [[ ! "$current_version" =~ ^([0-9]+)\.([0-9]+)\.([0-9]+)$ ]]; then
  echo "Version '${current_version}' must match X.Y.Z" >&2
  exit 1
fi

major="${BASH_REMATCH[1]}"
minor="${BASH_REMATCH[2]}"
patch="${BASH_REMATCH[3]}"
next_version="${major}.${minor}.$((patch + 1))"

sed -i -E "s/^${PROPERTY_NAME}=.*/${PROPERTY_NAME}=${next_version}/" "$FILE_PATH"

echo "current_version=${current_version}"
echo "next_version=${next_version}"
