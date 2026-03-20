#!/usr/bin/env bash
set -euo pipefail

API_URL="${API_URL:-http://localhost:8080}"
TARGET_STOCK_QUANTITY="${TARGET_STOCK_QUANTITY:-100}"
POSTGRES_CONTAINER="${POSTGRES_CONTAINER:-openpos-postgres}"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
POS_APP_DIR="$PROJECT_ROOT/apps/pos-terminal"
ADMIN_APP_DIR="$PROJECT_ROOT/apps/admin-dashboard"
POS_PUBLIC_DIR="$POS_APP_DIR/public"
ADMIN_PUBLIC_DIR="$ADMIN_APP_DIR/public"

ORG_NAME="テスト株式会社"
ORG_BUSINESS_TYPE="RETAIL"
ORG_INVOICE_NUMBER="T1234567890123"
DEFAULT_STORE_KEY="渋谷店"
DEFAULT_TERMINAL_CODE="POS-SHIBUYA-01"

ORG_ID=""
DEFAULT_STORE_ID=""
DEFAULT_TERMINAL_ID=""
TRANSACTION_EVENTS_EMITTED=0

declare -A TAX_RATE_IDS=()
declare -A CATEGORY_IDS=()
declare -A STORE_IDS=()
declare -A TERMINAL_IDS=()
declare -A STAFF_IDS=()
declare -A PRODUCT_IDS=()
declare -a PRODUCT_BARCODES=()
declare -a STORE_KEYS=()

HTTP_STATUS=""
HTTP_BODY=""

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "ERROR: Missing required command: $1" >&2
    exit 1
  }
}

fail() {
  echo "ERROR: $*" >&2
  exit 1
}

warn() {
  echo "WARN: $*" >&2
}

section() {
  echo
  echo "--- $* ---"
}

is_uuid() {
  [[ "$1" =~ ^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$ ]]
}

load_env_value() {
  local file="$1"
  local key="$2"

  [[ -f "$file" ]] || return 0

  awk -F= -v key="$key" '$1 == key { print substr($0, index($0, "=") + 1) }' "$file" | tail -n 1
}

load_json_value() {
  local file="$1"
  local key="$2"

  [[ -f "$file" ]] || return 0

  jq -r --arg key "$key" '.[$key] // empty' "$file" 2>/dev/null || true
}

discover_existing_org_id() {
  local candidate=""
  local file=""

  if [[ -n "${ORG_ID:-}" ]] && is_uuid "${ORG_ID:-}"; then
    printf '%s\n' "$ORG_ID"
    return 0
  fi

  for file in \
    "$POS_APP_DIR/.env.development.local" \
    "$ADMIN_APP_DIR/.env.development.local"; do
    candidate="$(load_env_value "$file" "VITE_ORGANIZATION_ID")"
    if [[ -n "$candidate" ]] && is_uuid "$candidate"; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done

  for file in \
    "$POS_PUBLIC_DIR/demo-config.json" \
    "$ADMIN_PUBLIC_DIR/demo-config.json"; do
    candidate="$(load_json_value "$file" "organizationId")"
    if [[ -n "$candidate" ]] && is_uuid "$candidate"; then
      printf '%s\n' "$candidate"
      return 0
    fi
  done
}

discover_org_id_from_database() {
  if ! command -v docker >/dev/null 2>&1; then
    return 0
  fi

  if ! docker inspect "$POSTGRES_CONTAINER" >/dev/null 2>&1; then
    return 0
  fi

  docker exec "$POSTGRES_CONTAINER" \
    psql -U openpos -d openpos -Atqc \
    "SELECT id FROM store_schema.organizations WHERE invoice_number = '$ORG_INVOICE_NUMBER' ORDER BY created_at DESC LIMIT 1;" \
    2>/dev/null | head -n 1 || true
}

api_request() {
  local method="$1"
  local path="$2"
  local body="${3:-}"
  local include_org="${4:-1}"
  local body_file=""
  local -a curl_args=()

  body_file="$(mktemp)"
  curl_args=(-sS -o "$body_file" -w "%{http_code}" -X "$method" "$API_URL$path")

  if [[ "$method" != "GET" && "$method" != "DELETE" ]]; then
    curl_args+=(-H "Content-Type: application/json")
  fi

  if [[ "$include_org" == "1" ]]; then
    [[ -n "$ORG_ID" ]] || fail "ORG_ID is not set for $method $path"
    curl_args+=(-H "X-Organization-Id: $ORG_ID")
  fi

  if [[ -n "$body" ]]; then
    curl_args+=(-d "$body")
  fi

  HTTP_STATUS="$(curl "${curl_args[@]}")"
  HTTP_BODY="$(cat "$body_file")"
  rm -f "$body_file"
}

expect_json() {
  local label="$1"
  shift

  api_request "$@"
  case "$HTTP_STATUS" in
    2??)
      printf '%s' "$HTTP_BODY"
      ;;
    *)
      fail "$label failed ($HTTP_STATUS): $HTTP_BODY"
      ;;
  esac
}

api_get() {
  expect_json "GET $1" GET "$1" "" 1
}

api_get_no_org() {
  expect_json "GET $1" GET "$1" "" 0
}

api_post() {
  expect_json "POST $1" POST "$1" "$2" 1
}

api_post_no_org() {
  expect_json "POST $1" POST "$1" "$2" 0
}

api_put() {
  expect_json "PUT $1" PUT "$1" "$2" 1
}

api_delete() {
  expect_json "DELETE $1" DELETE "$1" "" 1
}

api_get_optional() {
  api_request GET "$1" "" 1
  case "$HTTP_STATUS" in
    2??)
      return 0
      ;;
    404)
      return 1
      ;;
    *)
      fail "GET $1 failed ($HTTP_STATUS): $HTTP_BODY"
      ;;
  esac
}

extract_id() {
  jq -er '.id' <<<"$1"
}

ensure_organization() {
  local existing_org_id=""
  local existing_org_id_from_db=""
  local payload=""
  local response=""

  section "Ensuring organization"

  existing_org_id="$(discover_existing_org_id || true)"
  if [[ -n "$existing_org_id" ]]; then
    ORG_ID="$existing_org_id"
    if api_get_optional "/api/organizations/$ORG_ID"; then
      echo "Reusing existing organization: $ORG_ID"
    else
      warn "Stored organization id $ORG_ID does not exist anymore; creating a fresh demo organization."
      ORG_ID=""
    fi
  fi

  if [[ -z "$ORG_ID" ]]; then
    existing_org_id_from_db="$(discover_org_id_from_database)"
    if [[ -n "$existing_org_id_from_db" ]] && is_uuid "$existing_org_id_from_db"; then
      ORG_ID="$existing_org_id_from_db"
      if api_get_optional "/api/organizations/$ORG_ID"; then
        echo "Recovered existing organization by invoice number: $ORG_ID"
      else
        ORG_ID=""
      fi
    fi
  fi

  if [[ -z "$ORG_ID" ]]; then
    payload="$(
      jq -cn \
        --arg name "$ORG_NAME" \
        --arg businessType "$ORG_BUSINESS_TYPE" \
        --arg invoiceNumber "$ORG_INVOICE_NUMBER" \
        '{name: $name, businessType: $businessType, invoiceNumber: $invoiceNumber}'
    )"
    response="$(api_post_no_org "/api/organizations" "$payload")"
    ORG_ID="$(extract_id "$response")"
    echo "Created organization: $ORG_ID"
  fi

  payload="$(
    jq -cn \
      --arg name "$ORG_NAME" \
      --arg businessType "$ORG_BUSINESS_TYPE" \
      --arg invoiceNumber "$ORG_INVOICE_NUMBER" \
      '{name: $name, businessType: $businessType, invoiceNumber: $invoiceNumber}'
  )"
  response="$(api_put "/api/organizations/$ORG_ID" "$payload")"
  ORG_ID="$(extract_id "$response")"

  echo "Organization ready: $ORG_NAME ($ORG_ID)"
}

ensure_tax_rates() {
  local tax_rates_json=""
  local name=""
  local rate=""
  local is_reduced=""
  local is_default=""
  local id=""
  local payload=""
  local response=""

  section "Ensuring tax rates"

  tax_rates_json="$(api_get "/api/tax-rates")"

  while IFS='|' read -r name rate is_reduced is_default; do
    [[ -n "$name" ]] || continue

    id="$(
      jq -er --arg name "$name" '.[] | select(.name == $name) | .id' <<<"$tax_rates_json" 2>/dev/null | head -n 1 || true
    )"
    payload="$(
      jq -cn \
        --arg name "$name" \
        --arg rate "$rate" \
        --argjson isReduced "$is_reduced" \
        --argjson isDefault "$is_default" \
        '{name: $name, rate: $rate, isReduced: $isReduced, isDefault: $isDefault}'
    )"

    if [[ -n "$id" ]]; then
      response="$(api_put "/api/tax-rates/$id" "$payload")"
    else
      response="$(api_post "/api/tax-rates" "$payload")"
    fi

    id="$(extract_id "$response")"
    if [[ "$name" == "標準税率" ]]; then
      TAX_RATE_IDS["standard"]="$id"
    else
      TAX_RATE_IDS["reduced"]="$id"
    fi
    echo "Tax rate ready: $name ($id)"
  done <<'EOF'
標準税率|0.10|false|true
軽減税率|0.08|true|false
EOF
}

ensure_categories() {
  local categories_json=""
  local name=""
  local color=""
  local icon=""
  local display_order=""
  local tax_key=""
  local id=""
  local payload=""
  local response=""

  section "Ensuring categories"

  categories_json="$(api_get "/api/categories")"

  while IFS='|' read -r name tax_key color icon display_order; do
    [[ -n "$name" ]] || continue

    id="$(
      jq -er --arg name "$name" '.[] | select(.name == $name) | .id' <<<"$categories_json" 2>/dev/null | head -n 1 || true
    )"
    payload="$(
      jq -cn \
        --arg name "$name" \
        --arg color "$color" \
        --arg icon "$icon" \
        --argjson displayOrder "$display_order" \
        '{name: $name, color: $color, icon: $icon, displayOrder: $displayOrder}'
    )"

    if [[ -n "$id" ]]; then
      response="$(api_put "/api/categories/$id" "$payload")"
    else
      response="$(api_post "/api/categories" "$payload")"
    fi

    id="$(extract_id "$response")"
    CATEGORY_IDS["$name"]="$id"
    echo "Category ready: $name ($id, tax=$tax_key)"
  done <<'EOF'
食品|reduced|#F97316|utensils|10
飲料|reduced|#2563EB|cup-soda|20
日用品|standard|#10B981|package|30
衣類|standard|#7C3AED|shirt|40
EOF
}

ensure_stores() {
  local stores_json=""
  local name=""
  local address=""
  local phone=""
  local timezone=""
  local settings=""
  local id=""
  local payload=""
  local response=""

  section "Ensuring stores"

  stores_json="$(api_get "/api/stores?page=1&pageSize=100")"
  STORE_KEYS=("渋谷店" "新宿店")

  while IFS='|' read -r name address phone timezone; do
    [[ -n "$name" ]] || continue

    id="$(
      jq -er --arg name "$name" '.data[] | select(.name == $name) | .id' <<<"$stores_json" 2>/dev/null | head -n 1 || true
    )"
    settings="$(
      jq -cn \
        --arg locale "ja-JP" \
        --arg currency "JPY" \
        --arg channel "demo" \
        --arg storeName "$name" \
        '{locale: $locale, currency: $currency, channel: $channel, storeName: $storeName}'
    )"
    payload="$(
      jq -cn \
        --arg name "$name" \
        --arg address "$address" \
        --arg phone "$phone" \
        --arg timezone "$timezone" \
        --arg settings "$settings" \
        '{name: $name, address: $address, phone: $phone, timezone: $timezone, settings: $settings, isActive: true}'
    )"

    if [[ -n "$id" ]]; then
      response="$(api_put "/api/stores/$id" "$payload")"
    else
      response="$(api_post "/api/stores" "$payload")"
    fi

    id="$(extract_id "$response")"
    STORE_IDS["$name"]="$id"
    echo "Store ready: $name ($id)"
  done <<'EOF'
渋谷店|東京都渋谷区神南1-2-3|03-1234-5678|Asia/Tokyo
新宿店|東京都新宿区西新宿1-4-5|03-9876-5432|Asia/Tokyo
EOF

  DEFAULT_STORE_ID="${STORE_IDS[$DEFAULT_STORE_KEY]}"
}

ensure_terminals_for_store() {
  local store_name="$1"
  local store_id="$2"
  local terminals_json=""
  local terminal_code=""
  local terminal_name=""
  local id=""
  local payload=""
  local response=""

  terminals_json="$(api_get "/api/stores/$store_id/terminals")"

  while IFS='|' read -r terminal_code terminal_name; do
    [[ -n "$terminal_code" ]] || continue

    id="$(
      jq -er --arg code "$terminal_code" '.[] | select(.terminalCode == $code) | .id' <<<"$terminals_json" 2>/dev/null | head -n 1 || true
    )"

    if [[ -z "$id" ]]; then
      payload="$(
        jq -cn \
          --arg terminalCode "$terminal_code" \
          --arg name "$terminal_name" \
          '{terminalCode: $terminalCode, name: $name}'
      )"
      response="$(api_post "/api/stores/$store_id/terminals" "$payload")"
      id="$(extract_id "$response")"
    fi

    TERMINAL_IDS["$terminal_code"]="$id"
    echo "Terminal ready: $store_name / $terminal_code ($id)"
  done
}

ensure_terminals() {
  section "Ensuring terminals"

  ensure_terminals_for_store "渋谷店" "${STORE_IDS["渋谷店"]}" <<'EOF'
POS-SHIBUYA-01|渋谷店 レジ1
POS-SHIBUYA-02|渋谷店 レジ2
EOF

  ensure_terminals_for_store "新宿店" "${STORE_IDS["新宿店"]}" <<'EOF'
POS-SHINJUKU-01|新宿店 レジ1
POS-SHINJUKU-02|新宿店 レジ2
EOF

  DEFAULT_TERMINAL_ID="${TERMINAL_IDS[$DEFAULT_TERMINAL_CODE]}"
}

ensure_staff_for_store() {
  local store_name="$1"
  local store_id="$2"
  local staff_json=""
  local name=""
  local email=""
  local role=""
  local pin=""
  local id=""
  local payload=""
  local response=""

  staff_json="$(api_get "/api/staff?storeId=$store_id&page=1&pageSize=100")"

  while IFS='|' read -r name email role pin; do
    [[ -n "$email" ]] || continue

    id="$(
      jq -er --arg email "$email" '.data[] | select(.email == $email) | .id' <<<"$staff_json" 2>/dev/null | head -n 1 || true
    )"
    payload="$(
      jq -cn \
        --arg storeId "$store_id" \
        --arg name "$name" \
        --arg email "$email" \
        --arg role "$role" \
        --arg pin "$pin" \
        '{storeId: $storeId, name: $name, email: $email, role: $role, pin: $pin, isActive: true}'
    )"

    if [[ -n "$id" ]]; then
      response="$(api_put "/api/staff/$id" "$payload")"
    else
      response="$(api_post "/api/staff" "$payload")"
    fi

    id="$(extract_id "$response")"
    STAFF_IDS["$email"]="$id"
    echo "Staff ready: $store_name / $role / $email ($id)"
  done
}

ensure_staff() {
  section "Ensuring staff"

  ensure_staff_for_store "渋谷店" "${STORE_IDS["渋谷店"]}" <<'EOF'
渋谷店 オーナー|owner-shibuya@example.com|OWNER|1234
渋谷店 マネージャー|manager-shibuya@example.com|MANAGER|2345
渋谷店 レジ担当|cashier-shibuya@example.com|CASHIER|3456
EOF

  ensure_staff_for_store "新宿店" "${STORE_IDS["新宿店"]}" <<'EOF'
新宿店 オーナー|owner-shinjuku@example.com|OWNER|1234
新宿店 マネージャー|manager-shinjuku@example.com|MANAGER|2345
新宿店 レジ担当|cashier-shinjuku@example.com|CASHIER|3456
EOF
}

ensure_product() {
  local category_name="$1"
  local tax_key="$2"
  local barcode="$3"
  local sku="$4"
  local name="$5"
  local price="$6"
  local image_slug="$7"
  local description="$8"
  local display_order="$9"
  local response=""
  local payload=""
  local category_id=""
  local tax_rate_id=""
  local image_url=""

  category_id="${CATEGORY_IDS[$category_name]}"
  tax_rate_id="${TAX_RATE_IDS[$tax_key]}"

  # Use bundled demo-safe SVG images instead of external placeholders
  case "$category_name" in
    食品)   image_url="/demo-images/food.svg" ;;
    飲料)   image_url="/demo-images/beverage.svg" ;;
    日用品) image_url="/demo-images/daily.svg" ;;
    衣類)   image_url="/demo-images/clothing.svg" ;;
    *)      image_url="/demo-images/daily.svg" ;;
  esac

  if api_get_optional "/api/products/barcode/$barcode"; then
    payload="$(
      jq -cn \
        --arg name "$name" \
        --arg description "$description" \
        --arg barcode "$barcode" \
        --arg sku "$sku" \
        --arg categoryId "$category_id" \
        --arg taxRateId "$tax_rate_id" \
        --arg imageUrl "$image_url" \
        --argjson price "$price" \
        --argjson displayOrder "$display_order" \
        '{name: $name, description: $description, barcode: $barcode, sku: $sku, price: $price, categoryId: $categoryId, taxRateId: $taxRateId, imageUrl: $imageUrl, displayOrder: $displayOrder, isActive: true}'
    )"
    response="$(api_put "/api/products/$(extract_id "$HTTP_BODY")" "$payload")"
  else
    payload="$(
      jq -cn \
        --arg name "$name" \
        --arg description "$description" \
        --arg barcode "$barcode" \
        --arg sku "$sku" \
        --arg categoryId "$category_id" \
        --arg taxRateId "$tax_rate_id" \
        --arg imageUrl "$image_url" \
        --argjson price "$price" \
        --argjson displayOrder "$display_order" \
        '{name: $name, description: $description, barcode: $barcode, sku: $sku, price: $price, categoryId: $categoryId, taxRateId: $taxRateId, imageUrl: $imageUrl, displayOrder: $displayOrder}'
    )"
    response="$(api_post "/api/products" "$payload")"
  fi

  PRODUCT_IDS["$barcode"]="$(extract_id "$response")"
  PRODUCT_BARCODES+=("$barcode")
  echo "Product ready: $sku / $name (${PRODUCT_IDS[$barcode]})"
}

ensure_products() {
  section "Ensuring products"

  PRODUCT_BARCODES=()

  while IFS='|' read -r category_name tax_key barcode sku name price image_slug description display_order; do
    [[ -n "$barcode" ]] || continue
    ensure_product "$category_name" "$tax_key" "$barcode" "$sku" "$name" "$price" "$image_slug" "$description" "$display_order"
  done <<'EOF'
食品|reduced|4900000000001|FOOD-001|北海道おにぎり鮭|17800|food-001|北海道産鮭を使った定番おにぎり|10
食品|reduced|4900000000002|FOOD-002|ツナマヨおにぎり|16800|food-002|ツナとマヨネーズの人気おにぎり|20
食品|reduced|4900000000003|FOOD-003|和風幕の内弁当|59800|food-003|煮物と焼き魚を詰めた和風弁当|30
食品|reduced|4900000000004|FOOD-004|国産鶏からあげ弁当|64800|food-004|ジューシーなからあげが主役の弁当|40
食品|reduced|4900000000005|FOOD-005|玉子サンド|34800|food-005|ふんわり玉子を挟んだサンドイッチ|50
食品|reduced|4900000000006|FOOD-006|ミックスサンド|39800|food-006|ハムと野菜を組み合わせたサンドイッチ|60
食品|reduced|4900000000007|FOOD-007|海老グラタン|45800|food-007|海老の旨みが入ったクリーミーグラタン|70
食品|reduced|4900000000008|FOOD-008|ミネストローネスープ|24800|food-008|野菜をたっぷり使った温かいスープ|80
食品|reduced|4900000000009|FOOD-009|牛すじカレー|52000|food-009|牛すじの旨みを感じるスパイシーカレー|90
食品|reduced|4900000000010|FOOD-010|塩むすびセット|22800|food-010|軽食向けの塩むすびセット|100
飲料|reduced|4900000000011|BEV-001|緑茶|15800|bev-001|すっきり飲みやすい国産緑茶|10
飲料|reduced|4900000000012|BEV-002|烏龍茶|15800|bev-002|香ばしさがある定番の烏龍茶|20
飲料|reduced|4900000000013|BEV-003|天然水|11800|bev-003|毎日飲みやすい軟水ミネラルウォーター|30
飲料|reduced|4900000000014|BEV-004|カフェラテ|23800|bev-004|ミルク感のあるカフェラテ|40
飲料|reduced|4900000000015|BEV-005|ドリップコーヒー|18800|bev-005|深煎りの香りが広がるコーヒー|50
飲料|reduced|4900000000016|BEV-006|オレンジジュース|19800|bev-006|果汁感のあるオレンジジュース|60
飲料|reduced|4900000000017|BEV-007|炭酸水レモン|14800|bev-007|レモンが香る無糖炭酸水|70
飲料|reduced|4900000000018|BEV-008|エナジードリンク|24800|bev-008|気分転換向けのエナジードリンク|80
飲料|reduced|4900000000019|BEV-009|ヨーグルトドリンク|17800|bev-009|やさしい甘さのヨーグルトドリンク|90
飲料|reduced|4900000000020|BEV-010|アイスティー|16800|bev-010|すっきりした無糖アイスティー|100
日用品|standard|4900000000021|DAILY-001|キッチンペーパー|29800|daily-001|吸水性の高いキッチンペーパー|10
日用品|standard|4900000000022|DAILY-002|ティッシュ|22800|daily-002|日常使いしやすいソフトティッシュ|20
日用品|standard|4900000000023|DAILY-003|単三電池4本|45800|daily-003|常備しやすいアルカリ電池4本セット|30
日用品|standard|4900000000024|DAILY-004|歯ブラシセット|19800|daily-004|旅行にも便利な歯ブラシセット|40
日用品|standard|4900000000025|DAILY-005|洗濯ネット|24800|daily-005|衣類を守るメッシュ洗濯ネット|50
日用品|standard|4900000000026|DAILY-006|ハンドソープ|36800|daily-006|やさしい香りのハンドソープ|60
日用品|standard|4900000000027|DAILY-007|メモノートA5|18800|daily-007|使いやすいA5サイズのメモノート|70
日用品|standard|4900000000028|DAILY-008|油性ボールペン|12800|daily-008|滑らかに書ける油性ボールペン|80
日用品|standard|4900000000029|DAILY-009|USB-Cケーブル|98000|daily-009|充電とデータ転送に使えるUSB-Cケーブル|90
日用品|standard|4900000000030|DAILY-010|折りたたみ傘|128000|daily-010|急な雨に備えやすい軽量折りたたみ傘|100
衣類|standard|4900000000031|WEAR-001|ベーシックTシャツ|149000|wear-001|着回しやすいコットンTシャツ|10
衣類|standard|4900000000032|WEAR-002|リブソックス|59000|wear-002|毎日使いやすいリブ編みソックス|20
衣類|standard|4900000000033|WEAR-003|スウェットパーカー|399000|wear-003|ゆったり着られるスウェットパーカー|30
衣類|standard|4900000000034|WEAR-004|デニムキャップ|189000|wear-004|カジュアルに合わせやすいデニムキャップ|40
衣類|standard|4900000000035|WEAR-005|エコバッグ|89000|wear-005|持ち運びしやすい折りたたみエコバッグ|50
衣類|standard|4900000000036|WEAR-006|ワークエプロン|249000|wear-006|ポケット付きのワークエプロン|60
衣類|standard|4900000000037|WEAR-007|ルームスリッパ|129000|wear-007|室内で使いやすいクッションスリッパ|70
衣類|standard|4900000000038|WEAR-008|ストレッチレギンス|219000|wear-008|動きやすいストレッチレギンス|80
衣類|standard|4900000000039|WEAR-009|コットンハンカチ|69000|wear-009|やわらかなコットンハンカチ|90
衣類|standard|4900000000040|WEAR-010|ライトジャケット|599000|wear-010|季節の変わり目に使いやすいライトジャケット|100
EOF
}

ensure_store_inventory_target() {
  local store_name="$1"
  local store_id="$2"
  local note="$3"
  local stocks_json=""
  local barcode=""
  local product_id=""
  local current_quantity=""
  local quantity_change=""
  local payload=""

  stocks_json="$(api_get "/api/inventory/stocks?storeId=$store_id&page=1&pageSize=200")"

  for barcode in "${PRODUCT_BARCODES[@]}"; do
    product_id="${PRODUCT_IDS[$barcode]}"
    current_quantity="$(
      jq -er --arg productId "$product_id" '.data[] | select(.productId == $productId) | .quantity' <<<"$stocks_json" 2>/dev/null | head -n 1 || true
    )"
    current_quantity="${current_quantity:-0}"
    quantity_change=$((TARGET_STOCK_QUANTITY - current_quantity))

    if [[ "$quantity_change" -eq 0 ]]; then
      continue
    fi

    payload="$(
      jq -cn \
        --arg storeId "$store_id" \
        --arg productId "$product_id" \
        --arg note "$note" \
        --arg referenceId "stock-$barcode" \
        --argjson quantityChange "$quantity_change" \
        '{storeId: $storeId, productId: $productId, quantityChange: $quantityChange, movementType: "ADJUSTMENT", referenceId: $referenceId, note: $note}'
    )"
    api_post "/api/inventory/stocks/adjust" "$payload" >/dev/null
  done

  echo "Inventory normalized: $store_name -> $TARGET_STOCK_QUANTITY per product"
}

ensure_inventory_targets() {
  local note="$1"
  local store_name=""

  section "Ensuring inventory"

  for store_name in "${STORE_KEYS[@]}"; do
    ensure_store_inventory_target "$store_name" "${STORE_IDS[$store_name]}" "$note"
  done
}

ensure_transaction_items() {
  local transaction_id="$1"
  shift
  local transaction_json=""
  local item_json=""
  local item_id=""
  local product_id=""
  local target_quantity=""
  local current_quantity=""
  local barcode=""
  declare -A expected_quantities=()

  transaction_json="$(api_get "/api/transactions/$transaction_id")"

  while [[ "$#" -gt 0 ]]; do
    barcode="$1"
    target_quantity="$2"
    shift 2

    product_id="${PRODUCT_IDS[$barcode]:-}"
    [[ -n "$product_id" ]] || fail "Unknown product barcode in transaction seed: $barcode"
    expected_quantities["$product_id"]="$target_quantity"
  done

  while IFS= read -r item_json; do
    [[ -n "$item_json" ]] || continue

    item_id="$(jq -r '.id' <<<"$item_json")"
    product_id="$(jq -r '.productId' <<<"$item_json")"

    if [[ -z "${expected_quantities[$product_id]+x}" ]]; then
      api_delete "/api/transactions/$transaction_id/items/$item_id" >/dev/null
    fi
  done < <(jq -c '.items[]?' <<<"$transaction_json")

  transaction_json="$(api_get "/api/transactions/$transaction_id")"

  for product_id in "${!expected_quantities[@]}"; do
    target_quantity="${expected_quantities[$product_id]}"
    item_json="$(jq -c --arg productId "$product_id" '.items[]? | select(.productId == $productId)' <<<"$transaction_json" | head -n 1)"

    if [[ -z "$item_json" ]]; then
      transaction_json="$(
        api_post \
          "/api/transactions/$transaction_id/items" \
          "$(jq -cn --arg productId "$product_id" --argjson quantity "$target_quantity" '{productId: $productId, quantity: $quantity}')"
      )"
      continue
    fi

    current_quantity="$(jq -r '.quantity' <<<"$item_json")"
    if [[ "$current_quantity" != "$target_quantity" ]]; then
      item_id="$(jq -r '.id' <<<"$item_json")"
      transaction_json="$(
        api_put \
          "/api/transactions/$transaction_id/items/$item_id" \
          "$(jq -cn --argjson quantity "$target_quantity" '{quantity: $quantity}')"
      )"
    fi
  done

  printf '%s' "$transaction_json"
}

finalize_transaction() {
  local transaction_id="$1"
  local payment_method="$2"
  local reference="$3"
  local transaction_json=""
  local total=""
  local finalize_payload=""

  transaction_json="$(api_get "/api/transactions/$transaction_id")"
  total="$(jq -er '.total' <<<"$transaction_json")"

  case "$payment_method" in
    CASH)
      finalize_payload="$(
        jq -cn \
          --argjson amount "$total" \
          '{payments: [{method: "CASH", amount: $amount, received: $amount}]}'
      )"
      ;;
    CREDIT_CARD)
      finalize_payload="$(
        jq -cn \
          --arg reference "$reference" \
          --argjson amount "$total" \
          '{payments: [{method: "CREDIT_CARD", amount: $amount, reference: $reference}]}'
      )"
      ;;
    QR_CODE)
      finalize_payload="$(
        jq -cn \
          --arg reference "$reference" \
          --argjson amount "$total" \
          '{payments: [{method: "QR_CODE", amount: $amount, reference: $reference}]}'
      )"
      ;;
    *)
      fail "Unsupported payment method: $payment_method"
      ;;
  esac

  api_post "/api/transactions/$transaction_id/finalize" "$finalize_payload" | jq -c '.transaction'
}

ensure_sample_transaction() {
  local label="$1"
  local client_id="$2"
  local store_id="$3"
  local terminal_id="$4"
  local staff_id="$5"
  local desired_status="$6"
  local payment_method="$7"
  local reference="$8"
  local void_reason="$9"
  shift 9
  local create_payload=""
  local transaction_json=""
  local transaction_id=""
  local current_status=""

  create_payload="$(
    jq -cn \
      --arg storeId "$store_id" \
      --arg terminalId "$terminal_id" \
      --arg staffId "$staff_id" \
      --arg clientId "$client_id" \
      '{storeId: $storeId, terminalId: $terminalId, staffId: $staffId, type: "SALE", clientId: $clientId}'
  )"
  transaction_json="$(api_post "/api/transactions" "$create_payload")"
  transaction_id="$(extract_id "$transaction_json")"
  current_status="$(jq -r '.status' <<<"$transaction_json")"

  if [[ "$current_status" == "DRAFT" ]]; then
    transaction_json="$(ensure_transaction_items "$transaction_id" "$@")"
    current_status="$(jq -r '.status' <<<"$transaction_json")"
  fi

  case "$desired_status" in
    DRAFT)
      if [[ "$current_status" != "DRAFT" ]]; then
        warn "$label is $current_status; keeping existing immutable sample transaction."
      fi
      ;;
    COMPLETED)
      case "$current_status" in
        DRAFT)
          transaction_json="$(finalize_transaction "$transaction_id" "$payment_method" "$reference")"
          TRANSACTION_EVENTS_EMITTED=1
          ;;
        COMPLETED)
          ;;
        VOIDED)
          warn "$label is already VOIDED; keeping existing immutable sample transaction."
          ;;
        *)
          fail "$label has unexpected transaction status: $current_status"
          ;;
      esac
      ;;
    VOIDED)
      case "$current_status" in
        DRAFT)
          transaction_json="$(finalize_transaction "$transaction_id" "$payment_method" "$reference")"
          current_status="$(jq -r '.status' <<<"$transaction_json")"
          TRANSACTION_EVENTS_EMITTED=1
          ;&
        COMPLETED)
          transaction_json="$(
            api_post \
              "/api/transactions/$transaction_id/void" \
              "$(jq -cn --arg reason "$void_reason" '{reason: $reason}')"
          )"
          TRANSACTION_EVENTS_EMITTED=1
          ;;
        VOIDED)
          ;;
        *)
          fail "$label has unexpected transaction status: $current_status"
          ;;
      esac
      ;;
    *)
      fail "Unsupported desired transaction status: $desired_status"
      ;;
  esac

  echo "Transaction ready: $label ($(jq -r '.status' <<<"$transaction_json"))"
}

ensure_sample_transactions() {
  section "Ensuring sample transactions"

  ensure_sample_transaction \
    "渋谷店 現金会計" \
    "seed-sale-001" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-01"]}" \
    "${STAFF_IDS["cashier-shibuya@example.com"]}" \
    "COMPLETED" \
    "CASH" \
    "seed-cash-001" \
    "" \
    "4900000000001" 2 \
    "4900000000011" 1

  ensure_sample_transaction \
    "渋谷店 カード会計" \
    "seed-sale-002" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-02"]}" \
    "${STAFF_IDS["manager-shibuya@example.com"]}" \
    "COMPLETED" \
    "CREDIT_CARD" \
    "seed-card-002" \
    "" \
    "4900000000021" 1 \
    "4900000000031" 1

  ensure_sample_transaction \
    "新宿店 QR会計" \
    "seed-sale-003" \
    "${STORE_IDS["新宿店"]}" \
    "${TERMINAL_IDS["POS-SHINJUKU-01"]}" \
    "${STAFF_IDS["cashier-shinjuku@example.com"]}" \
    "COMPLETED" \
    "QR_CODE" \
    "seed-qr-003" \
    "" \
    "4900000000004" 1 \
    "4900000000014" 1 \
    "4900000000028" 2

  ensure_sample_transaction \
    "渋谷店 VOID取引" \
    "seed-sale-004" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-01"]}" \
    "${STAFF_IDS["manager-shibuya@example.com"]}" \
    "VOIDED" \
    "CASH" \
    "seed-cash-004" \
    "お客様都合による取消" \
    "4900000000002" 1 \
    "4900000000012" 1

  ensure_sample_transaction \
    "渋谷店 下書き1" \
    "seed-sale-005" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-01"]}" \
    "${STAFF_IDS["cashier-shibuya@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000005" 1

  ensure_sample_transaction \
    "渋谷店 下書き2" \
    "seed-sale-006" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-02"]}" \
    "${STAFF_IDS["owner-shibuya@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000017" 2 \
    "4900000000010" 1

  ensure_sample_transaction \
    "新宿店 下書き1" \
    "seed-sale-007" \
    "${STORE_IDS["新宿店"]}" \
    "${TERMINAL_IDS["POS-SHINJUKU-02"]}" \
    "${STAFF_IDS["owner-shinjuku@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000032" 3

  ensure_sample_transaction \
    "新宿店 下書き2" \
    "seed-sale-008" \
    "${STORE_IDS["新宿店"]}" \
    "${TERMINAL_IDS["POS-SHINJUKU-01"]}" \
    "${STAFF_IDS["manager-shinjuku@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000023" 1 \
    "4900000000029" 1

  ensure_sample_transaction \
    "渋谷店 下書き3" \
    "seed-sale-009" \
    "${STORE_IDS["渋谷店"]}" \
    "${TERMINAL_IDS["POS-SHIBUYA-01"]}" \
    "${STAFF_IDS["cashier-shibuya@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000008" 1 \
    "4900000000019" 1

  ensure_sample_transaction \
    "新宿店 下書き3" \
    "seed-sale-010" \
    "${STORE_IDS["新宿店"]}" \
    "${TERMINAL_IDS["POS-SHINJUKU-02"]}" \
    "${STAFF_IDS["cashier-shinjuku@example.com"]}" \
    "DRAFT" \
    "" \
    "" \
    "" \
    "4900000000040" 1 \
    "4900000000024" 1
}

settle_inventory_after_transactions() {
  local pass=""
  local store_name=""

  if [[ "$TRANSACTION_EVENTS_EMITTED" -eq 0 ]]; then
    return 0
  fi

  section "Normalizing inventory after sample transactions"

  for pass in 1 2 3; do
    sleep 2
    for store_name in "${STORE_KEYS[@]}"; do
      ensure_store_inventory_target \
        "$store_name" \
        "${STORE_IDS[$store_name]}" \
        "開発シード在庫を$TARGET_STOCK_QUANTITYに正規化 (pass $pass)"
    done
  done
}

write_runtime_config() {
  section "Writing frontend runtime config"

  mkdir -p "$POS_PUBLIC_DIR" "$ADMIN_PUBLIC_DIR"

  cat > "$POS_APP_DIR/.env.development.local" <<EOF
VITE_API_URL=$API_URL
VITE_ORGANIZATION_ID=$ORG_ID
VITE_DEFAULT_STORE_ID=$DEFAULT_STORE_ID
VITE_DEFAULT_TERMINAL_ID=$DEFAULT_TERMINAL_ID
EOF

  cat > "$ADMIN_APP_DIR/.env.development.local" <<EOF
VITE_API_URL=$API_URL
VITE_ORGANIZATION_ID=$ORG_ID
EOF

  cat > "$POS_PUBLIC_DIR/demo-config.json" <<EOF
{
  "apiUrl": "$API_URL",
  "organizationId": "$ORG_ID",
  "storeId": "$DEFAULT_STORE_ID",
  "terminalId": "$DEFAULT_TERMINAL_ID"
}
EOF

  cat > "$ADMIN_PUBLIC_DIR/demo-config.json" <<EOF
{
  "apiUrl": "$API_URL",
  "organizationId": "$ORG_ID"
}
EOF

  echo "Runtime config updated for POS and admin apps."
}

print_summary() {
  section "Seed summary"

  echo "Organization: $ORG_NAME ($ORG_ID)"
  echo "Invoice No.:  $ORG_INVOICE_NUMBER"
  echo "Default store / terminal:"
  echo "  $DEFAULT_STORE_KEY -> $DEFAULT_STORE_ID"
  echo "  $DEFAULT_TERMINAL_CODE -> $DEFAULT_TERMINAL_ID"
  echo "Stores:"
  echo "  渋谷店    ${STORE_IDS["渋谷店"]}"
  echo "  新宿店    ${STORE_IDS["新宿店"]}"
  echo "Staff PINs:"
  echo "  OWNER   1234"
  echo "  MANAGER 2345"
  echo "  CASHIER 3456"
  echo "Products: ${#PRODUCT_BARCODES[@]}"
  echo "Sample transactions: 10 total (COMPLETED 3 / VOIDED 1 / DRAFT 6)"
  echo "Inventory: normalized to $TARGET_STOCK_QUANTITY per product in both stores"
}

require_command curl
require_command jq

echo "=== open-pos demo seed ==="
echo "API: $API_URL"

ensure_organization
ensure_tax_rates
ensure_categories
ensure_stores
ensure_terminals
ensure_staff
ensure_products
ensure_inventory_targets "開発シード初期在庫を投入"
ensure_sample_transactions
settle_inventory_after_transactions
write_runtime_config
print_summary
