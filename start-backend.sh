#!/usr/bin/env bash
# Start ONLY the Spring Boot backend (http://localhost:8080).
# Delegates to backend/run.sh, which loads backend/.env.local (DB password, etc.).
set -euo pipefail
cd "$(dirname "$0")/backend"
exec ./run.sh
