#!/usr/bin/env bash
# Start ONLY the React (Vite) dev server (http://localhost:5173).
set -euo pipefail
cd "$(dirname "$0")"

if [[ ! -d node_modules ]]; then
  echo "node_modules missing — running npm install..."
  npm install
fi

exec npm run dev
