# gRPCurl Examples

gRPC Reflection が dev プロファイルで有効化されているため、`-plaintext` オプションでローカルサービスに直接接続できます。

## 前提条件

- gRPCurl がインストール済み (`brew install grpcurl` / `go install github.com/fullstorydev/grpcurl/cmd/grpcurl@latest`)
- 各サービスが dev プロファイルで起動済み (`./gradlew :services:{name}:quarkusDev`)
- 組織ID（テナント識別子）をメタデータで渡す必要がある

## ポート一覧

| サービス | gRPC ポート |
|---------|-----------|
| product-service | 9001 |
| store-service | 9002 |
| pos-service | 9003 |
| inventory-service | 9004 |
| analytics-service | 9005 |

## 共通メタデータ

全 RPC には `x-organization-id` メタデータが必要です。以下の例ではデモ用組織IDを使用します。

```bash
ORG_ID="00000000-0000-0000-0000-000000000001"
```

## サービス一覧の確認

```bash
# 全サービスの RPC 一覧を確認
grpcurl -plaintext localhost:9001 list
grpcurl -plaintext localhost:9002 list
grpcurl -plaintext localhost:9003 list
grpcurl -plaintext localhost:9004 list
grpcurl -plaintext localhost:9005 list

# 特定サービスのメソッド一覧
grpcurl -plaintext localhost:9001 list openpos.product.v1.ProductService
```

---

## ProductService (port 9001)

### 商品作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "name": "テスト商品",
    "price": 10000,
    "barcode": "4901234567890",
    "category_id": "",
    "tax_rate_id": ""
  }' \
  localhost:9001 openpos.product.v1.ProductService/CreateProduct
```

### 商品取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"id": "PRODUCT_UUID"}' \
  localhost:9001 openpos.product.v1.ProductService/GetProduct
```

### 商品一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "pagination": {"page": 1, "page_size": 20},
    "active_only": true
  }' \
  localhost:9001 openpos.product.v1.ProductService/ListProducts
```

### 商品検索（フリーワード）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "pagination": {"page": 1, "page_size": 20},
    "search": "コーヒー",
    "active_only": true
  }' \
  localhost:9001 openpos.product.v1.ProductService/ListProducts
```

### バーコードで商品検索

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"barcode": "4901234567890"}' \
  localhost:9001 openpos.product.v1.ProductService/GetProductByBarcode
```

### 商品更新

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "id": "PRODUCT_UUID",
    "name": "更新後の商品名",
    "price": 15000
  }' \
  localhost:9001 openpos.product.v1.ProductService/UpdateProduct
```

### 商品削除（論理削除）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"id": "PRODUCT_UUID"}' \
  localhost:9001 openpos.product.v1.ProductService/DeleteProduct
```

### カテゴリ作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "name": "飲料",
    "color": "#3B82F6",
    "display_order": 1
  }' \
  localhost:9001 openpos.product.v1.ProductService/CreateCategory
```

### カテゴリ一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{}' \
  localhost:9001 openpos.product.v1.ProductService/ListCategories
```

### 税率作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "name": "標準税率10%",
    "rate": "0.10",
    "is_reduced": false,
    "is_default": true
  }' \
  localhost:9001 openpos.product.v1.ProductService/CreateTaxRate
```

### 税率一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{}' \
  localhost:9001 openpos.product.v1.ProductService/ListTaxRates
```

### 割引作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "name": "10%OFF セール",
    "discount_type": "DISCOUNT_TYPE_PERCENTAGE",
    "value": "10.0"
  }' \
  localhost:9001 openpos.product.v1.ProductService/CreateDiscount
```

### 割引一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"active_only": true}' \
  localhost:9001 openpos.product.v1.ProductService/ListDiscounts
```

### クーポン作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "code": "SUMMER2026",
    "discount_id": "DISCOUNT_UUID",
    "max_uses": 100
  }' \
  localhost:9001 openpos.product.v1.ProductService/CreateCoupon
```

### クーポン検証

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"code": "SUMMER2026"}' \
  localhost:9001 openpos.product.v1.ProductService/ValidateCoupon
```

---

## StoreService (port 9002)

### 組織作成

```bash
grpcurl -plaintext \
  -d '{
    "name": "テスト株式会社",
    "business_type": "retail",
    "invoice_number": "T1234567890123"
  }' \
  localhost:9002 openpos.store.v1.StoreService/CreateOrganization
```

### 組織取得

```bash
grpcurl -plaintext \
  -d '{"id": "ORG_UUID"}' \
  localhost:9002 openpos.store.v1.StoreService/GetOrganization
```

### 店舗作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "name": "渋谷店",
    "address": "東京都渋谷区...",
    "phone": "03-1234-5678",
    "timezone": "Asia/Tokyo"
  }' \
  localhost:9002 openpos.store.v1.StoreService/CreateStore
```

### 店舗取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"id": "STORE_UUID"}' \
  localhost:9002 openpos.store.v1.StoreService/GetStore
```

### 店舗一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"pagination": {"page": 1, "page_size": 20}}' \
  localhost:9002 openpos.store.v1.StoreService/ListStores
```

### 端末登録

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "terminal_code": "POS-01",
    "name": "レジ1番"
  }' \
  localhost:9002 openpos.store.v1.StoreService/RegisterTerminal
```

### 端末一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"store_id": "STORE_UUID"}' \
  localhost:9002 openpos.store.v1.StoreService/ListTerminals
```

### スタッフ作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "name": "田中太郎",
    "email": "tanaka@example.com",
    "role": "STAFF_ROLE_CASHIER",
    "pin": "1234"
  }' \
  localhost:9002 openpos.store.v1.StoreService/CreateStaff
```

### スタッフ一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9002 openpos.store.v1.StoreService/ListStaff
```

### PIN 認証

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "staff_id": "STAFF_UUID",
    "pin": "1234"
  }' \
  localhost:9002 openpos.store.v1.StoreService/AuthenticateByPin
```

---

## PosService (port 9003)

### 取引作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "terminal_id": "TERMINAL_UUID",
    "staff_id": "STAFF_UUID",
    "type": "TRANSACTION_TYPE_SALE"
  }' \
  localhost:9003 openpos.pos.v1.PosService/CreateTransaction
```

### 取引取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"id": "TRANSACTION_UUID"}' \
  localhost:9003 openpos.pos.v1.PosService/GetTransaction
```

### 取引一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "pagination": {"page": 1, "page_size": 20},
    "store_id": "STORE_UUID"
  }' \
  localhost:9003 openpos.pos.v1.PosService/ListTransactions
```

### 取引一覧取得（ステータスフィルタ）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "pagination": {"page": 1, "page_size": 20},
    "store_id": "STORE_UUID",
    "status": "TRANSACTION_STATUS_COMPLETED"
  }' \
  localhost:9003 openpos.pos.v1.PosService/ListTransactions
```

### 明細追加

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "product_id": "PRODUCT_UUID",
    "quantity": 2
  }' \
  localhost:9003 openpos.pos.v1.PosService/AddTransactionItem
```

### 明細更新（数量変更）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "item_id": "ITEM_UUID",
    "quantity": 3
  }' \
  localhost:9003 openpos.pos.v1.PosService/UpdateTransactionItem
```

### 明細削除

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "item_id": "ITEM_UUID"
  }' \
  localhost:9003 openpos.pos.v1.PosService/RemoveTransactionItem
```

### 取引確定（現金払い）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "payments": [{
      "method": "PAYMENT_METHOD_CASH",
      "amount": 10000,
      "received": 20000
    }]
  }' \
  localhost:9003 openpos.pos.v1.PosService/FinalizeTransaction
```

### 取引確定（クレジットカード）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "payments": [{
      "method": "PAYMENT_METHOD_CREDIT_CARD",
      "amount": 10000,
      "reference": "****1234"
    }]
  }' \
  localhost:9003 openpos.pos.v1.PosService/FinalizeTransaction
```

### 取引確定（分割払い: 現金 + QR）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "payments": [
      {"method": "PAYMENT_METHOD_CASH", "amount": 5000, "received": 5000},
      {"method": "PAYMENT_METHOD_QR_CODE", "amount": 5000, "reference": "PP-ABC123"}
    ]
  }' \
  localhost:9003 openpos.pos.v1.PosService/FinalizeTransaction
```

### 取引無効化

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "transaction_id": "TRANSACTION_UUID",
    "reason": "お客様都合によるキャンセル"
  }' \
  localhost:9003 openpos.pos.v1.PosService/VoidTransaction
```

### レシート取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"transaction_id": "TRANSACTION_UUID"}' \
  localhost:9003 openpos.pos.v1.PosService/GetReceipt
```

---

## InventoryService (port 9004)

### 在庫取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "product_id": "PRODUCT_UUID"
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/GetStock
```

### 在庫一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListStocks
```

### 在庫一覧取得（低在庫のみ）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "pagination": {"page": 1, "page_size": 20},
    "low_stock_only": true
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListStocks
```

### 在庫調整（手動）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "product_id": "PRODUCT_UUID",
    "quantity_change": 10,
    "movement_type": "MOVEMENT_TYPE_ADJUSTMENT",
    "note": "棚卸による調整"
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/AdjustStock
```

### 在庫調整（返品）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "product_id": "PRODUCT_UUID",
    "quantity_change": 1,
    "movement_type": "MOVEMENT_TYPE_RETURN",
    "reference_id": "TRANSACTION_UUID",
    "note": "返品による在庫復元"
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/AdjustStock
```

### 在庫移動履歴取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListStockMovements
```

### 在庫移動履歴取得（商品指定）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "product_id": "PRODUCT_UUID",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListStockMovements
```

### 発注作成

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "supplier_name": "テスト卸株式会社",
    "note": "定期発注",
    "items": [
      {"product_id": "PRODUCT_UUID_1", "ordered_quantity": 100, "unit_cost": 5000},
      {"product_id": "PRODUCT_UUID_2", "ordered_quantity": 50, "unit_cost": 8000}
    ]
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/CreatePurchaseOrder
```

### 発注取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{"id": "PO_UUID"}' \
  localhost:9004 openpos.inventory.v1.InventoryService/GetPurchaseOrder
```

### 発注一覧取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListPurchaseOrders
```

### 発注一覧取得（ステータスフィルタ）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "status": "PURCHASE_ORDER_STATUS_DRAFT",
    "pagination": {"page": 1, "page_size": 20}
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/ListPurchaseOrders
```

### 発注ステータス更新（発注済みに変更）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "id": "PO_UUID",
    "status": "PURCHASE_ORDER_STATUS_ORDERED"
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/UpdatePurchaseOrderStatus
```

### 発注ステータス更新（入荷確認）

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "id": "PO_UUID",
    "status": "PURCHASE_ORDER_STATUS_RECEIVED",
    "received_items": [
      {"product_id": "PRODUCT_UUID_1", "received_quantity": 100},
      {"product_id": "PRODUCT_UUID_2", "received_quantity": 48}
    ]
  }' \
  localhost:9004 openpos.inventory.v1.InventoryService/UpdatePurchaseOrderStatus
```

---

## AnalyticsService (port 9005)

### 日次売上取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "date_range": {
      "start": "2026-03-01T00:00:00Z",
      "end": "2026-03-15T23:59:59Z"
    }
  }' \
  localhost:9005 openpos.analytics.v1.AnalyticsService/GetDailySales
```

### 商品別売上取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "date_range": {
      "start": "2026-03-01T00:00:00Z",
      "end": "2026-03-15T23:59:59Z"
    },
    "pagination": {"page": 1, "page_size": 20},
    "sort_by": "amount"
  }' \
  localhost:9005 openpos.analytics.v1.AnalyticsService/GetProductSales
```

### 時間帯別売上取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "date": "2026-03-15"
  }' \
  localhost:9005 openpos.analytics.v1.AnalyticsService/GetHourlySales
```

### 売上サマリー取得

```bash
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{
    "store_id": "STORE_UUID",
    "date_range": {
      "start": "2026-03-01T00:00:00Z",
      "end": "2026-03-31T23:59:59Z"
    }
  }' \
  localhost:9005 openpos.analytics.v1.AnalyticsService/GetSalesSummary
```

---

## Tips

### Proto メッセージのスキーマ確認

```bash
# メッセージのフィールド定義を確認
grpcurl -plaintext localhost:9001 describe openpos.product.v1.Product
grpcurl -plaintext localhost:9003 describe openpos.pos.v1.Transaction

# Enum の値を確認
grpcurl -plaintext localhost:9003 describe openpos.pos.v1.TransactionType
```

### JSON 出力のフォーマット

```bash
# jq でフォーマット
grpcurl -plaintext \
  -H "x-organization-id: $ORG_ID" \
  -d '{}' \
  localhost:9001 openpos.product.v1.ProductService/ListTaxRates | jq .
```

### エラーハンドリング

gRPC のエラーレスポンスは以下のステータスコードで返されます:

| ステータス | 意味 |
|-----------|------|
| `INVALID_ARGUMENT` | リクエストパラメータが不正 |
| `NOT_FOUND` | 指定リソースが存在しない |
| `FAILED_PRECONDITION` | 前提条件を満たしていない（例: DRAFT でない取引に明細追加） |
| `PERMISSION_DENIED` | テナント権限なし |
| `UNAUTHENTICATED` | `x-organization-id` ヘッダーなし |
