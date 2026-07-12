#!/usr/bin/env bash
set -euo pipefail

WEB_UI_URL="${WEB_UI_URL:-http://localhost:5173}"
CURL_NO_PROXY="${CURL_NO_PROXY:-localhost,127.0.0.1}"

page_file="$(mktemp)"
cleanup() {
  rm -f "$page_file"
}
trap cleanup EXIT

echo "Checking web UI at ${WEB_UI_URL}"
curl --noproxy "$CURL_NO_PROXY" -fsS "$WEB_UI_URL" > "$page_file"

if ! grep -q '<div id="root"' "$page_file"; then
  echo "Web UI responded, but the page does not look like the Vite/React app shell:" >&2
  head -40 "$page_file" >&2
  exit 1
fi

echo "Web UI smoke check passed."
