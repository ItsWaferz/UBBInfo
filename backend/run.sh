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

# Resolve JAVA_HOME only if it isn't already set. No hardcoded path, so this
# works on Linux CI / prod as well as macOS. If nothing is found we leave it
# unset and let mvn use the `java` already on PATH.
if [[ -z "${JAVA_HOME:-}" ]]; then
  if [[ -x /usr/libexec/java_home ]]; then          # macOS
    JAVA_HOME="$(/usr/libexec/java_home 2>/dev/null || true)"
  elif command -v java >/dev/null 2>&1; then         # Linux / generic
    _java="$(command -v java)"
    command -v realpath >/dev/null 2>&1 && _java="$(realpath "$_java")"
    JAVA_HOME="${_java%/bin/java}"
  fi
  [[ -n "${JAVA_HOME:-}" ]] && export JAVA_HOME
fi
exec mvn spring-boot:run
