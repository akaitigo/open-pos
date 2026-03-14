#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

ORG_NAME="テスト株式会社"
ORG_INVOICE_NUMBER="T1234567890123"

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

load_json_value() {
  local file="$1"
  local key="$2"

  [[ -f "$file" ]] || return 0
  jq -r --arg key "$key" '.[$key] // empty' "$file" 2>/dev/null || true
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
ORG_ID="${ORG_ID:-${VITE_ORGANIZATION_ID:-$(load_json_value "$PROJECT_ROOT/apps/admin-dashboard/public/demo-config.json" "organizationId")}}"
STORE_ID="${STORE_ID:-${VITE_DEFAULT_STORE_ID:-$(load_json_value "$PROJECT_ROOT/apps/pos-terminal/public/demo-config.json" "storeId")}}"
TERMINAL_ID="${TERMINAL_ID:-${VITE_DEFAULT_TERMINAL_ID:-$(load_json_value "$PROJECT_ROOT/apps/pos-terminal/public/demo-config.json" "terminalId")}}"

[[ -n "$ORG_ID" ]] || fail "ORG_ID is not set. Run scripts/seed.sh first."
[[ -n "$STORE_ID" ]] || fail "STORE_ID is not set. Run scripts/seed.sh first."
[[ -n "$TERMINAL_ID" ]] || fail "TERMINAL_ID is not set. Run scripts/seed.sh first."

health_json="$(curl -fsS "$API_URL/api/health")"
[[ "$(jq -r '.status' <<<"$health_json")" == "UP" ]] || fail "api-gateway health check failed"
pass "api-gateway health"

organization_json="$(api_get "/api/organizations/$ORG_ID")"
[[ "$(jq -r '.id' <<<"$organization_json")" == "$ORG_ID" ]] || fail "organization lookup failed"
[[ "$(jq -r '.name' <<<"$organization_json")" == "$ORG_NAME" ]] || fail "organization name mismatch"
[[ "$(jq -r '.invoiceNumber' <<<"$organization_json")" == "$ORG_INVOICE_NUMBER" ]] || fail "organization invoice mismatch"
pass "organization $ORG_NAME"

tax_rates_json="$(api_get "/api/tax-rates")"
[[ "$(jq '[.[] | select(.name == "標準税率" and ((.rate | tonumber) == 0.1) and .isDefault == true)] | length' <<<"$tax_rates_json")" -eq 1 ]] ||
  fail "standard tax rate is missing"
[[ "$(jq '[.[] | select(.name == "軽減税率" and ((.rate | tonumber) == 0.08) and .isReduced == true)] | length' <<<"$tax_rates_json")" -eq 1 ]] ||
  fail "reduced tax rate is missing"
pass "tax rates"

categories_json="$(api_get "/api/categories")"
for category_name in "食品" "飲料" "日用品" "衣類"; do
  jq -e --arg name "$category_name" '.[] | select(.name == $name)' <<<"$categories_json" >/dev/null ||
    fail "category $category_name is missing"
done
pass "required categories"

food_category_id="$(jq -r '.[] | select(.name == "食品") | .id' <<<"$categories_json")"
drink_category_id="$(jq -r '.[] | select(.name == "飲料") | .id' <<<"$categories_json")"
daily_category_id="$(jq -r '.[] | select(.name == "日用品") | .id' <<<"$categories_json")"
wear_category_id="$(jq -r '.[] | select(.name == "衣類") | .id' <<<"$categories_json")"

products_json="$(api_get "/api/products?page=1&pageSize=200")"
product_total="$(jq '.pagination.totalCount' <<<"$products_json")"
(( product_total >= 40 )) || fail "expected at least 40 products, got $product_total"
[[ "$(jq --arg id "$food_category_id" '[.data[] | select(.categoryId == $id)] | length' <<<"$products_json")" -ge 10 ]] || fail "食品 products are missing"
[[ "$(jq --arg id "$drink_category_id" '[.data[] | select(.categoryId == $id)] | length' <<<"$products_json")" -ge 10 ]] || fail "飲料 products are missing"
[[ "$(jq --arg id "$daily_category_id" '[.data[] | select(.categoryId == $id)] | length' <<<"$products_json")" -ge 10 ]] || fail "日用品 products are missing"
[[ "$(jq --arg id "$wear_category_id" '[.data[] | select(.categoryId == $id)] | length' <<<"$products_json")" -ge 10 ]] || fail "衣類 products are missing"
jq -e '.data[] | select(.barcode == "4900000000001" and .sku == "FOOD-001" and .name == "北海道おにぎり鮭")' <<<"$products_json" >/dev/null ||
  fail "known seeded product is missing"
pass "products ($product_total)"

stores_json="$(api_get "/api/stores?page=1&pageSize=100")"
shibuya_store_id="$(jq -r '.data[] | select(.name == "渋谷店") | .id' <<<"$stores_json" | head -n 1)"
shinjuku_store_id="$(jq -r '.data[] | select(.name == "新宿店") | .id' <<<"$stores_json" | head -n 1)"
[[ -n "$shibuya_store_id" ]] || fail "渋谷店 was not found"
[[ -n "$shinjuku_store_id" ]] || fail "新宿店 was not found"
[[ "$STORE_ID" == "$shibuya_store_id" ]] || fail "default seeded store should point to 渋谷店"
pass "stores"

shibuya_terminals_json="$(api_get "/api/stores/$shibuya_store_id/terminals")"
shinjuku_terminals_json="$(api_get "/api/stores/$shinjuku_store_id/terminals")"
jq -e '.[] | select(.terminalCode == "POS-SHIBUYA-01")' <<<"$shibuya_terminals_json" >/dev/null ||
  fail "POS-SHIBUYA-01 is missing"
jq -e '.[] | select(.terminalCode == "POS-SHIBUYA-02")' <<<"$shibuya_terminals_json" >/dev/null ||
  fail "POS-SHIBUYA-02 is missing"
jq -e '.[] | select(.terminalCode == "POS-SHINJUKU-01")' <<<"$shinjuku_terminals_json" >/dev/null ||
  fail "POS-SHINJUKU-01 is missing"
jq -e '.[] | select(.terminalCode == "POS-SHINJUKU-02")' <<<"$shinjuku_terminals_json" >/dev/null ||
  fail "POS-SHINJUKU-02 is missing"
[[ "$TERMINAL_ID" == "$(jq -r '.[] | select(.terminalCode == "POS-SHIBUYA-01") | .id' <<<"$shibuya_terminals_json")" ]] ||
  fail "default seeded terminal should point to POS-SHIBUYA-01"
pass "terminals"

shibuya_staff_json="$(api_get "/api/staff?storeId=$shibuya_store_id&page=1&pageSize=100")"
shinjuku_staff_json="$(api_get "/api/staff?storeId=$shinjuku_store_id&page=1&pageSize=100")"

for email in \
  "owner-shibuya@example.com" \
  "manager-shibuya@example.com" \
  "cashier-shibuya@example.com"; do
  jq -e --arg email "$email" '.data[] | select(.email == $email)' <<<"$shibuya_staff_json" >/dev/null ||
    fail "staff $email is missing from 渋谷店"
done

for email in \
  "owner-shinjuku@example.com" \
  "manager-shinjuku@example.com" \
  "cashier-shinjuku@example.com"; do
  jq -e --arg email "$email" '.data[] | select(.email == $email)' <<<"$shinjuku_staff_json" >/dev/null ||
    fail "staff $email is missing from 新宿店"
done

shibuya_cashier_id="$(jq -r '.data[] | select(.email == "cashier-shibuya@example.com") | .id' <<<"$shibuya_staff_json")"
shinjuku_manager_id="$(jq -r '.data[] | select(.email == "manager-shinjuku@example.com") | .id' <<<"$shinjuku_staff_json")"

auth_cashier_json="$(api_post "/api/staff/$shibuya_cashier_id/authenticate" "{\"storeId\":\"$shibuya_store_id\",\"pin\":\"3456\"}")"
[[ "$(jq -r '.success' <<<"$auth_cashier_json")" == "true" ]] || fail "渋谷店 cashier PIN authentication failed"

auth_manager_json="$(api_post "/api/staff/$shinjuku_manager_id/authenticate" "{\"storeId\":\"$shinjuku_store_id\",\"pin\":\"2345\"}")"
[[ "$(jq -r '.success' <<<"$auth_manager_json")" == "true" ]] || fail "新宿店 manager PIN authentication failed"
pass "staff + PIN auth"

shibuya_stocks_json="$(api_get "/api/inventory/stocks?storeId=$shibuya_store_id&page=1&pageSize=200")"
shinjuku_stocks_json="$(api_get "/api/inventory/stocks?storeId=$shinjuku_store_id&page=1&pageSize=200")"
[[ "$(jq '.pagination.totalCount' <<<"$shibuya_stocks_json")" -ge 40 ]] || fail "渋谷店 inventory is missing"
[[ "$(jq '.pagination.totalCount' <<<"$shinjuku_stocks_json")" -ge 40 ]] || fail "新宿店 inventory is missing"
[[ "$(jq '[.data[] | select(.quantity == 100)] | length' <<<"$shibuya_stocks_json")" -ge 40 ]] || fail "渋谷店 inventory was not normalized to 100"
[[ "$(jq '[.data[] | select(.quantity == 100)] | length' <<<"$shinjuku_stocks_json")" -ge 40 ]] || fail "新宿店 inventory was not normalized to 100"
pass "inventory"

transactions_json="$(api_get "/api/transactions?page=1&pageSize=200")"
(( "$(jq '.pagination.totalCount' <<<"$transactions_json")" >= 10 )) || fail "expected at least 10 transactions"

for spec in \
  "seed-sale-001:COMPLETED" \
  "seed-sale-002:COMPLETED" \
  "seed-sale-003:COMPLETED" \
  "seed-sale-004:VOIDED" \
  "seed-sale-005:DRAFT" \
  "seed-sale-006:DRAFT" \
  "seed-sale-007:DRAFT" \
  "seed-sale-008:DRAFT" \
  "seed-sale-009:DRAFT" \
  "seed-sale-010:DRAFT"; do
  client_id="${spec%%:*}"
  expected_status="${spec##*:}"
  jq -e --arg clientId "$client_id" --arg status "$expected_status" '.data[] | select(.clientId == $clientId and .status == $status)' <<<"$transactions_json" >/dev/null ||
    fail "transaction $client_id with status $expected_status is missing"
done

[[ "$(jq '[.data[] | select((.clientId // "") | startswith("seed-sale-")) | select(.status == "COMPLETED")] | length' <<<"$transactions_json")" -ge 3 ]] ||
  fail "expected at least 3 completed sample transactions"
[[ "$(jq '[.data[] | select((.clientId // "") | startswith("seed-sale-")) | select(.status == "VOIDED")] | length' <<<"$transactions_json")" -ge 1 ]] ||
  fail "expected at least 1 voided sample transaction"
pass "sample transactions"

echo "Local demo smoke passed."
