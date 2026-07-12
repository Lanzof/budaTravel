#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
ORIGIN_STOP_ID="${ORIGIN_STOP_ID:-F00985}"
DESTINATION_STOP_ID="${DESTINATION_STOP_ID:-F00045}"
DEPARTURE_DATE_TIME="${DEPARTURE_DATE_TIME:-2026-01-27T04:44:00+01:00}"
CURL_NO_PROXY="${CURL_NO_PROXY:-localhost,127.0.0.1}"

readiness_file="$(mktemp)"
route_file="$(mktemp)"
cleanup() {
  rm -f "$readiness_file" "$route_file"
}
trap cleanup EXIT

echo "Checking API readiness at ${API_BASE_URL}/api/v1/readiness"
curl --noproxy "$CURL_NO_PROXY" -fsS "${API_BASE_URL}/api/v1/readiness" > "$readiness_file"

if ! grep -q '"ready"[[:space:]]*:[[:space:]]*true' "$readiness_file"; then
  echo "API is reachable, but demo data is not ready yet:" >&2
  cat "$readiness_file" >&2
  exit 1
fi

echo "Searching demo route ${ORIGIN_STOP_ID} -> ${DESTINATION_STOP_ID}"
status_code="$({
  cat <<JSON
{
  "originStopId": "${ORIGIN_STOP_ID}",
  "destinationStopId": "${DESTINATION_STOP_ID}",
  "departureDateTime": "${DEPARTURE_DATE_TIME}",
  "optimization": "FASTEST",
  "transportTypes": ["BUS"]
}
JSON
} | curl --noproxy "$CURL_NO_PROXY" -sS -o "$route_file" -w '%{http_code}' \
  -H 'Content-Type: application/json' \
  -d @- \
  "${API_BASE_URL}/api/v1/routes/search")"

if [[ "$status_code" != "200" ]]; then
  echo "Route search failed with HTTP ${status_code}:" >&2
  cat "$route_file" >&2
  exit 1
fi

if ! grep -q '"segments"[[:space:]]*:' "$route_file"; then
  echo "Route search returned 200, but response does not contain route segments:" >&2
  cat "$route_file" >&2
  exit 1
fi

echo "Demo route API smoke check passed."
