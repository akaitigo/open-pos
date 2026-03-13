#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

load_env_file() {
  local file="$1"
  if [[ -f "$file" ]]; then
    set -a
    # shellcheck disable=SC1090
    source "$file"
    set +a
  fi
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

pass() {
  echo "OK  $*"
}

api_get() {
  local path="$1"
  curl -fsS "$API_URL$path" -H "X-Organization-Id: $ORG_ID"
}

api_post() {
  local path="$1"
  local body="$2"
  curl -fsS -X POST "$API_URL$path" \
    -H "Content-Type: application/json" \
    -H "X-Organization-Id: $ORG_ID" \
    -d "$body"
}

require_command curl
require_command jq

load_env_file "$PROJECT_ROOT/apps/admin-dashboard/.env.development.local"
load_env_file "$PROJECT_ROOT/apps/pos-terminal/.env.development.local"

API_URL="${API_URL:-${VITE_API_URL:-http://localhost:8080}}"
ORG_ID="${ORG_ID:-${VITE_ORGANIZATION_ID:-}}"
STORE_ID="${STORE_ID:-${VITE_DEFAULT_STORE_ID:-}}"
TERMINAL_ID="${TERMINAL_ID:-${VITE_DEFAULT_TERMINAL_ID:-}}"

[[ -n "$ORG_ID" ]] || fail "ORG_ID is not set. Run scripts/seed.sh first."
[[ -n "$STORE_ID" ]] || fail "STORE_ID is not set. Run scripts/seed.sh first."
[[ -n "$TERMINAL_ID" ]] || fail "TERMINAL_ID is not set. Run scripts/seed.sh first."

health_json="$(curl -fsS "$API_URL/api/health")"
[[ "$(jq -r '.status' <<<"$health_json")" == "UP" ]] || fail "api-gateway health check failed"
pass "api-gateway health"

organization_json="$(api_get "/api/organizations/$ORG_ID")"
[[ "$(jq -r '.id' <<<"$organization_json")" == "$ORG_ID" ]] || fail "organization lookup failed"
pass "organization $ORG_ID"

categories_json="$(api_get "/api/categories")"
categories_count="$(jq 'length' <<<"$categories_json")"
(( categories_count >= 6 )) || fail "expected at least 6 categories, got $categories_count"
pass "categories ($categories_count)"

tax_rates_json="$(api_get "/api/tax-rates")"
tax_rate_count="$(jq 'length' <<<"$tax_rates_json")"
(( tax_rate_count >= 2 )) || fail "expected at least 2 tax rates, got $tax_rate_count"
pass "tax rates ($tax_rate_count)"

products_json="$(api_get "/api/products?page=1&pageSize=100")"
product_total="$(jq '.pagination.totalCount' <<<"$products_json")"
(( product_total >= 17 )) || fail "expected at least 17 products, got $product_total"
jq -e '.data[] | select(.taxRateId != null and .taxRateId != "")' <<<"$products_json" >/dev/null ||
  fail "expected at least one product with a tax rate"
pass "products ($product_total)"

stores_json="$(api_get "/api/stores?page=1&pageSize=100")"
jq -e --arg store_id "$STORE_ID" '.data[] | select(.id == $store_id)' <<<"$stores_json" >/dev/null ||
  fail "seeded store $STORE_ID was not found"
pass "store $STORE_ID"

terminals_json="$(api_get "/api/stores/$STORE_ID/terminals")"
jq -e --arg terminal_id "$TERMINAL_ID" '.[] | select(.id == $terminal_id and .terminalCode == "POS-001")' \
  <<<"$terminals_json" >/dev/null || fail "seeded terminal $TERMINAL_ID was not found"
pass "terminal $TERMINAL_ID"

staff_json="$(api_get "/api/staff?storeId=$STORE_ID&page=1&pageSize=100")"
staff_total="$(jq '.pagination.totalCount' <<<"$staff_json")"
(( staff_total >= 2 )) || fail "expected at least 2 staff, got $staff_total"
manager_id="$(jq -r '.data[] | select(.email == "tanaka@example.com") | .id' <<<"$staff_json" | head -n 1)"
[[ -n "$manager_id" ]] || fail "seeded manager was not found"
pass "staff ($staff_total)"

auth_json="$(api_post "/api/staff/$manager_id/authenticate" "{\"storeId\":\"$STORE_ID\",\"pin\":\"1234\"}")"
[[ "$(jq -r '.success' <<<"$auth_json")" == "true" ]] || fail "manager PIN authentication failed"
pass "manager PIN authentication"

echo "Local demo smoke passed."
