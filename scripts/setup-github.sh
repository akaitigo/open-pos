#!/usr/bin/env bash
set -euo pipefail

REPO="akaitigo/open-pos"

command -v gh >/dev/null 2>&1 || {
  echo "gh CLI is required" >&2
  exit 1
}

echo "=== Deleting default labels ==="
gh label list --repo "$REPO" --json name -q '.[].name' | while read -r label; do
  gh label delete "$label" --repo "$REPO" --yes 2>/dev/null || true
done

echo "=== Creating labels ==="
# サービス別（紫系）
gh label create "svc:api-gateway"  --repo "$REPO" --color "7B68EE" --description "API Gateway"
gh label create "svc:pos"          --repo "$REPO" --color "9370DB" --description "POS Service"
gh label create "svc:product"      --repo "$REPO" --color "8A2BE2" --description "Product Service"
gh label create "svc:inventory"    --repo "$REPO" --color "9932CC" --description "Inventory Service"
gh label create "svc:analytics"    --repo "$REPO" --color "BA55D3" --description "Analytics Service"
gh label create "svc:store"        --repo "$REPO" --color "DDA0DD" --description "Store Service"
gh label create "app:pos-terminal" --repo "$REPO" --color "6A5ACD" --description "POS端末 PWA"
gh label create "app:admin"        --repo "$REPO" --color "483D8B" --description "管理画面 SPA"
gh label create "infra"            --repo "$REPO" --color "4B0082" --description "インフラ・Docker・CI"
gh label create "proto"            --repo "$REPO" --color "663399" --description "Protobuf 定義"

# タイプ別
gh label create "type:feature" --repo "$REPO" --color "0E8A16" --description "新機能"
gh label create "type:bug"     --repo "$REPO" --color "D73A4A" --description "バグ修正"
gh label create "type:chore"   --repo "$REPO" --color "FEF2C0" --description "メンテナンス"
gh label create "type:docs"    --repo "$REPO" --color "0075CA" --description "ドキュメント"
gh label create "type:test"    --repo "$REPO" --color "BFD4F2" --description "テスト"

# 優先度
gh label create "P0:critical" --repo "$REPO" --color "B60205" --description "最優先"
gh label create "P1:high"     --repo "$REPO" --color "FF6347" --description "高優先"
gh label create "P2:medium"   --repo "$REPO" --color "FBCA04" --description "中優先"
gh label create "P3:low"      --repo "$REPO" --color "C5DEF5" --description "低優先"

echo "=== Creating milestones ==="
gh api repos/"$REPO"/milestones --method POST -f title="Phase 0: Foundation" -f description="モノリポ・Docker・スケルトン・CI"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 1: Product & Store" -f description="商品管理 + 店舗管理 + 基本UI"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 2: POS Core" -f description="会計・決済・レシート"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 3: Offline" -f description="PWA オフライン対応・同期"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 4: Inventory" -f description="在庫管理 + イベント駆動"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 5: Analytics" -f description="売上分析ダッシュボード"
gh api repos/"$REPO"/milestones --method POST -f title="Phase 6: Polish" -f description="モック決済・返品・E2Eテスト"

echo "=== Done ==="
