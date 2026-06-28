#!/usr/bin/env bash
# Loads local secrets and starts the backend.
set -euo pipefail
cd "$(dirname "$0")"

if [[ -f .env.local ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env.local
  set +a
else
  echo "WARNING: backend/.env.local not found — DB password / JWT secret will be empty." >&2
fi

export JAVA_HOME="${JAVA_HOME:-/Library/Java/JavaVirtualMachines/temurin-25.jdk/Contents/Home}"
exec mvn spring-boot:run
