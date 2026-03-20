#!/usr/bin/env bash
# Run k6 load tests against the local open-pos stack.
# Requires: k6 (https://grafana.com/docs/k6/latest/set-up/install-k6/)
#
# Usage:
#   make load-test                       # run all scenarios
#   K6_SCENARIO=product-listing make load-test  # run a single scenario
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
K6_DIR="$PROJECT_ROOT/tests/k6"

if ! command -v k6 >/dev/null 2>&1; then
  echo "k6 is not installed." >&2
  echo "Install: https://grafana.com/docs/k6/latest/set-up/install-k6/" >&2
  exit 1
fi

# --- Auto-discover IDs from demo-config.json / env files ---
load_json_value() {
  local file="$1" key="$2"
  [[ -f "$file" ]] || return 0
  jq -r --arg key "$key" '.[$key] // empty' "$file" 2>/dev/null || true
}

load_env_value() {
  local file="$1" key="$2"
  [[ -f "$file" ]] || return 0
  awk -F= -v key="$key" '$1 == key { print substr($0, index($0, "=") + 1) }' "$file" | tail -n 1
}

POS_CONFIG="$PROJECT_ROOT/apps/pos-terminal/public/demo-config.json"
ADMIN_CONFIG="$PROJECT_ROOT/apps/admin-dashboard/public/demo-config.json"
POS_ENV="$PROJECT_ROOT/apps/pos-terminal/.env.development.local"

export K6_BASE_URL="${K6_BASE_URL:-http://localhost:8080}"
export K6_ORG_ID="${K6_ORG_ID:-$(load_json_value "$ADMIN_CONFIG" "organizationId")}"
export K6_STORE_ID="${K6_STORE_ID:-$(load_json_value "$POS_CONFIG" "storeId")}"
export K6_TERMINAL_ID="${K6_TERMINAL_ID:-$(load_json_value "$POS_CONFIG" "terminalId")}"

# Discover a staff ID for transactions
if [[ -z "${K6_STAFF_ID:-}" ]]; then
  if command -v curl >/dev/null 2>&1 && command -v jq >/dev/null 2>&1 && [[ -n "$K6_ORG_ID" ]] && [[ -n "$K6_STORE_ID" ]]; then
    K6_STAFF_ID="$(
      curl -fsS "$K6_BASE_URL/api/staff?storeId=$K6_STORE_ID&page=1&pageSize=1" \
        -H "X-Organization-Id: $K6_ORG_ID" 2>/dev/null \
      | jq -r '.data[0].id // empty' 2>/dev/null || true
    )"
  fi
fi
export K6_STAFF_ID="${K6_STAFF_ID:-}"

echo "=== open-pos k6 load test ==="
echo "Base URL:    $K6_BASE_URL"
echo "Org ID:      $K6_ORG_ID"
echo "Store ID:    $K6_STORE_ID"
echo "Terminal ID: $K6_TERMINAL_ID"
echo "Staff ID:    $K6_STAFF_ID"
echo ""

if [[ -z "$K6_ORG_ID" ]]; then
  echo "ERROR: K6_ORG_ID is not set. Run 'make local-demo' or set K6_ORG_ID manually." >&2
  exit 1
fi

SCENARIO="${K6_SCENARIO:-all}"
FAILURES=0

run_scenario() {
  local name="$1"
  local file="$K6_DIR/$name.js"

  if [[ ! -f "$file" ]]; then
    echo "ERROR: scenario file not found: $file" >&2
    return 1
  fi

  echo ""
  echo "--- Running: $name ---"
  if ! k6 run "$file"; then
    FAILURES=$((FAILURES + 1))
  fi
}

if [[ "$SCENARIO" == "all" ]]; then
  run_scenario "product-listing"
  run_scenario "transaction-create"
  run_scenario "transaction-finalize"
else
  run_scenario "$SCENARIO"
fi

echo ""
if [[ "$FAILURES" -gt 0 ]]; then
  echo "=== Load test finished with $FAILURES failure(s) ==="
  exit 1
fi

echo "=== All load tests passed ==="
