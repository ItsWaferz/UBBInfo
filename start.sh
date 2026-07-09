#!/usr/bin/env bash
# Start BOTH the Spring Boot backend and the React dev server.
# Backend runs in the background; the frontend runs in the foreground.
# Press Ctrl+C (or quit) to stop both.
set -euo pipefail
cd "$(dirname "$0")"

BACKEND_LOG="backend/backend.log"

# Stop ONLY the backend we started (its mvn process + forked java child) when
# this script exits — never a broad pkill/lsof that could hit unrelated processes.
BACKEND_PID=""
cleanup() {
  echo ""
  echo "Stopping backend..."
  if [ -n "$BACKEND_PID" ]; then
    pkill -P "$BACKEND_PID" 2>/dev/null || true   # the forked java child
    kill "$BACKEND_PID" 2>/dev/null || true        # the mvn process
    wait "$BACKEND_PID" 2>/dev/null || true
  fi
}
trap cleanup EXIT INT TERM

echo "Starting backend (logs -> $BACKEND_LOG)..."
./start-backend.sh > "$BACKEND_LOG" 2>&1 &
BACKEND_PID=$!

# Wait for the backend to come up on :8080 (timeout ~120s).
echo "Waiting for backend on http://localhost:8080 ..."
for _ in $(seq 1 120); do
  if lsof -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
    break
  fi
  # Surface an early backend crash instead of waiting the full timeout.
  if ! kill -0 "$BACKEND_PID" 2>/dev/null; then
    echo "Backend exited early. Last lines of $BACKEND_LOG:"
    tail -n 30 "$BACKEND_LOG" || true
    exit 1
  fi
  sleep 1
done

if ! lsof -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then
  echo "Backend did not start within the timeout. See $BACKEND_LOG."
  exit 1
fi

echo "Backend is up. Starting frontend (http://localhost:5173)..."
echo "----------------------------------------------------------------"
# Frontend in the foreground; when it stops, the trap stops the backend.
./start-frontend.sh
