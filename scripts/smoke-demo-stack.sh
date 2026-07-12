#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"

echo "Running budaTravel local demo stack smoke checks..."
"${SCRIPT_DIR}/smoke-web-ui.sh"
"${SCRIPT_DIR}/smoke-demo-route.sh"
echo "budaTravel local demo stack smoke checks passed."
