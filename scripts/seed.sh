#!/usr/bin/env bash
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
ORG_ID=""
INVOICE_NUMBER="T$(date +%s%N | cut -c1-13)"

# --- Helper Functions ---

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: Missing required command: $1" >&2
    exit 1
  }
}

api_post_no_org() {
  local path="$1"
  local data="$2"
  local body_file
  body_file="$(mktemp)"
  local http_code
  http_code=$(curl -sS -o "$body_file" -w "%{http_code}" -X POST "$API_URL$path" \
    -H "Content-Type: application/json" \
    -d "$data")
  local body
  body="$(cat "$body_file")"
  rm -f "$body_file"
  if [[ "$http_code" -ge 400 ]]; then
    echo "ERROR: POST $path returned $http_code" >&2
    echo "$body" >&2
    exit 1
  fi
  echo "$body"
}

api_post() {
  local path="$1"
  local data="$2"
  local body_file
  body_file="$(mktemp)"
  local http_code
  http_code=$(curl -sS -o "$body_file" -w "%{http_code}" -X POST "$API_URL$path" \
    -H "Content-Type: application/json" \
    -H "X-Organization-Id: $ORG_ID" \
    -d "$data")
  local body
  body="$(cat "$body_file")"
  rm -f "$body_file"
  if [[ "$http_code" -ge 400 ]]; then
    echo "ERROR: POST $path returned $http_code" >&2
    echo "$body" >&2
    exit 1
  fi
  echo "$body"
}

extract_id() {
  local id
  id=$(echo "$1" | jq -r '.id')
  if [[ -z "$id" || "$id" == "null" ]]; then
    echo "ERROR: Failed to extract id from response" >&2
    echo "$1" >&2
    exit 1
  fi
  echo "$id"
}

require_command bc
require_command curl
require_command jq

echo "=== open-pos Seed Data ==="
echo "API: $API_URL"
echo ""

# --- 1. Organization ---
echo "--- Creating Organization ---"
ORG_RESPONSE=$(api_post_no_org "/api/organizations" \
  "{\"name\": \"デモ株式会社\", \"businessType\": \"RETAIL\", \"invoiceNumber\": \"$INVOICE_NUMBER\"}")
ORG_ID=$(extract_id "$ORG_RESPONSE")
echo "Organization: $ORG_ID"

# --- 2. Tax Rates ---
echo ""
echo "--- Creating Tax Rates ---"

TAX_STANDARD_RESPONSE=$(api_post "/api/tax-rates" \
  '{"name": "標準税率", "rate": "0.10", "isDefault": true}')
TAX_STANDARD_ID=$(extract_id "$TAX_STANDARD_RESPONSE")
echo "Standard tax rate (10%): $TAX_STANDARD_ID"

TAX_REDUCED_RESPONSE=$(api_post "/api/tax-rates" \
  '{"name": "軽減税率", "rate": "0.08", "isReduced": true}')
TAX_REDUCED_ID=$(extract_id "$TAX_REDUCED_RESPONSE")
echo "Reduced tax rate (8%):  $TAX_REDUCED_ID"

# --- 3. Categories ---
echo ""
echo "--- Creating Categories ---"

CAT_BEVERAGE_RESPONSE=$(api_post "/api/categories" '{"name": "飲料"}')
CAT_BEVERAGE_ID=$(extract_id "$CAT_BEVERAGE_RESPONSE")
echo "Category 飲料:           $CAT_BEVERAGE_ID"

CAT_BREAD_RESPONSE=$(api_post "/api/categories" '{"name": "パン・サンドイッチ"}')
CAT_BREAD_ID=$(extract_id "$CAT_BREAD_RESPONSE")
echo "Category パン・サンドイッチ: $CAT_BREAD_ID"

CAT_BENTO_RESPONSE=$(api_post "/api/categories" '{"name": "弁当・おにぎり"}')
CAT_BENTO_ID=$(extract_id "$CAT_BENTO_RESPONSE")
echo "Category 弁当・おにぎり:   $CAT_BENTO_ID"

CAT_SNACK_RESPONSE=$(api_post "/api/categories" '{"name": "菓子"}')
CAT_SNACK_ID=$(extract_id "$CAT_SNACK_RESPONSE")
echo "Category 菓子:           $CAT_SNACK_ID"

CAT_DAILY_RESPONSE=$(api_post "/api/categories" '{"name": "日用品"}')
CAT_DAILY_ID=$(extract_id "$CAT_DAILY_RESPONSE")
echo "Category 日用品:         $CAT_DAILY_ID"

CAT_OTHER_RESPONSE=$(api_post "/api/categories" '{"name": "その他"}')
CAT_OTHER_ID=$(extract_id "$CAT_OTHER_RESPONSE")
echo "Category その他:         $CAT_OTHER_ID"

# --- 4. Products ---
echo ""
echo "--- Creating Products ---"

# Helper for product creation
create_product() {
  local name="$1"
  local price="$2"
  local category_id="$3"
  local tax_rate_id="$4"
  local response
  response=$(api_post "/api/products" \
    "{\"name\": \"$name\", \"price\": $price, \"categoryId\": \"$category_id\", \"taxRateId\": \"$tax_rate_id\"}")
  local id
  id=$(extract_id "$response")
  printf "  %-20s %6d (¥%s) → %s\n" "$name" "$price" "$(echo "scale=0; $price / 100" | bc)" "$id"
}

echo "  [飲料 - 軽減税率]"
create_product "ドリップコーヒー" 15000 "$CAT_BEVERAGE_ID" "$TAX_REDUCED_ID"
create_product "カフェラテ" 25000 "$CAT_BEVERAGE_ID" "$TAX_REDUCED_ID"
create_product "緑茶ペットボトル" 16000 "$CAT_BEVERAGE_ID" "$TAX_REDUCED_ID"

echo "  [パン・サンドイッチ - 軽減税率]"
create_product "たまごサンド" 35000 "$CAT_BREAD_ID" "$TAX_REDUCED_ID"
create_product "クロワッサン" 18000 "$CAT_BREAD_ID" "$TAX_REDUCED_ID"
create_product "メロンパン" 15000 "$CAT_BREAD_ID" "$TAX_REDUCED_ID"

echo "  [弁当・おにぎり - 軽減税率]"
create_product "幕の内弁当" 55000 "$CAT_BENTO_ID" "$TAX_REDUCED_ID"
create_product "おにぎり 鮭" 13000 "$CAT_BENTO_ID" "$TAX_REDUCED_ID"
create_product "おにぎり 梅" 12000 "$CAT_BENTO_ID" "$TAX_REDUCED_ID"

echo "  [菓子 - 軽減税率]"
create_product "チョコレートバー" 12000 "$CAT_SNACK_ID" "$TAX_REDUCED_ID"
create_product "ポテトチップス" 15000 "$CAT_SNACK_ID" "$TAX_REDUCED_ID"
create_product "ガム" 10000 "$CAT_SNACK_ID" "$TAX_REDUCED_ID"

echo "  [日用品 - 標準税率]"
create_product "ボールペン" 11000 "$CAT_DAILY_ID" "$TAX_STANDARD_ID"
create_product "ノート A5" 20000 "$CAT_DAILY_ID" "$TAX_STANDARD_ID"
create_product "乾電池 単3" 30000 "$CAT_DAILY_ID" "$TAX_STANDARD_ID"

echo "  [その他 - 標準税率]"
create_product "レジ袋" 300 "$CAT_OTHER_ID" "$TAX_STANDARD_ID"
create_product "切手 84円" 8400 "$CAT_OTHER_ID" "$TAX_STANDARD_ID"

# --- 5. Store ---
echo ""
echo "--- Creating Store ---"
STORE_RESPONSE=$(api_post "/api/stores" \
  '{"name": "渋谷本店", "address": "東京都渋谷区神南1-2-3", "phone": "03-1234-5678", "timezone": "Asia/Tokyo"}')
STORE_ID=$(extract_id "$STORE_RESPONSE")
echo "Store 渋谷本店: $STORE_ID"

# --- 6. Terminal ---
echo ""
echo "--- Creating Terminal ---"
TERMINAL_RESPONSE=$(api_post "/api/stores/$STORE_ID/terminals" \
  "{\"terminalCode\": \"POS-001\", \"name\": \"レジ1番\"}")
TERMINAL_ID=$(extract_id "$TERMINAL_RESPONSE")
echo "Terminal レジ1番: $TERMINAL_ID"

# --- 7. Staff ---
echo ""
echo "--- Creating Staff ---"
STAFF1_RESPONSE=$(api_post "/api/staff" \
  "{\"storeId\": \"$STORE_ID\", \"name\": \"田中太郎\", \"email\": \"tanaka@example.com\", \"role\": \"MANAGER\", \"pin\": \"1234\"}")
STAFF1_ID=$(extract_id "$STAFF1_RESPONSE")
echo "Staff 田中太郎 (MANAGER): $STAFF1_ID"

STAFF2_RESPONSE=$(api_post "/api/staff" \
  "{\"storeId\": \"$STORE_ID\", \"name\": \"山田花子\", \"email\": \"yamada@example.com\", \"role\": \"CASHIER\", \"pin\": \"5678\"}")
STAFF2_ID=$(extract_id "$STAFF2_RESPONSE")
echo "Staff 山田花子 (CASHIER): $STAFF2_ID"

# --- 8. Generate .env.development.local ---
echo ""
echo "--- Generating Frontend Demo Config ---"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
POS_PUBLIC_DIR="$PROJECT_ROOT/apps/pos-terminal/public"
ADMIN_PUBLIC_DIR="$PROJECT_ROOT/apps/admin-dashboard/public"

mkdir -p "$POS_PUBLIC_DIR" "$ADMIN_PUBLIC_DIR"

cat > "$PROJECT_ROOT/apps/pos-terminal/.env.development.local" << EOF
VITE_API_URL=http://localhost:8080
VITE_ORGANIZATION_ID=$ORG_ID
VITE_DEFAULT_STORE_ID=$STORE_ID
VITE_DEFAULT_TERMINAL_ID=$TERMINAL_ID
EOF
echo "  Created apps/pos-terminal/.env.development.local"

cat > "$PROJECT_ROOT/apps/admin-dashboard/.env.development.local" << EOF
VITE_API_URL=http://localhost:8080
VITE_ORGANIZATION_ID=$ORG_ID
EOF
echo "  Created apps/admin-dashboard/.env.development.local"

cat > "$POS_PUBLIC_DIR/demo-config.json" << EOF
{
  "apiUrl": "http://localhost:8080",
  "organizationId": "$ORG_ID",
  "storeId": "$STORE_ID",
  "terminalId": "$TERMINAL_ID"
}
EOF
echo "  Created apps/pos-terminal/public/demo-config.json"

cat > "$ADMIN_PUBLIC_DIR/demo-config.json" << EOF
{
  "apiUrl": "http://localhost:8080",
  "organizationId": "$ORG_ID"
}
EOF
echo "  Created apps/admin-dashboard/public/demo-config.json"

# --- 9. Summary ---
echo ""
echo "========================================="
echo "  Seed Data Complete!"
echo "========================================="
echo ""
echo "Organization:  $ORG_ID"
echo "Invoice No:    $INVOICE_NUMBER"
echo "Store:         $STORE_ID"
echo "Terminal:      $TERMINAL_ID"
echo "Staff:"
echo "  田中太郎 (MANAGER): $STAFF1_ID  PIN: 1234"
echo "  山田花子 (CASHIER): $STAFF2_ID  PIN: 5678"
echo ""
echo "Tax Rates:"
echo "  標準税率 (10%): $TAX_STANDARD_ID"
echo "  軽減税率 (8%):  $TAX_REDUCED_ID"
echo ""
echo "Categories: 飲料, パン・サンドイッチ, 弁当・おにぎり, 菓子, 日用品, その他"
echo "Products:   17 items created"
echo ""
echo "Frontend demo config generated for pos-terminal and admin-dashboard"
echo "========================================="
