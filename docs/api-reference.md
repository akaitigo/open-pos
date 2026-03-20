# API リファレンス

open-pos は api-gateway (BFF) を通じて REST API を公開し、サービス間通信には gRPC を使用しています。

## OpenAPI / Swagger

api-gateway には SmallRye OpenAPI (`quarkus-smallrye-openapi`) が組み込まれています。gateway の起動中は、以下の URL でインタラクティブなドキュメントを参照できます。

- **Swagger UI**: `http://localhost:8080/q/swagger-ui`
- **OpenAPI JSON**: `http://localhost:8080/q/openapi`

`services/api-gateway/src/main/kotlin/com/openpos/gateway/resource/` 配下の Resource クラスは MicroProfile OpenAPI の `@Tag` および `@Operation` アノテーションを使用しています。

## REST API エンドポイント

すべてのエンドポイントは `/api` プレフィックスが付きます。認証は `Authorization: Bearer {token}` (ORY Hydra 発行の JWT) を使用します。テナント分離は gateway が注入する `X-Organization-Id` ヘッダーにより自動的に行われます。

### Transactions (POS)

| Method | Path | 説明 | Role |
|--------|------|------|------|
| POST | `/api/transactions` | 取引を作成 | CASHIER+ |
| GET | `/api/transactions/{id}` | 取引詳細を取得 | CASHIER+ |
| GET | `/api/transactions` | 取引一覧を取得 | MANAGER+ |
| POST | `/api/transactions/{id}/void` | 取引を無効化 | MANAGER+ |
| POST | `/api/transactions/{id}/return` | 取引を返品処理 | MANAGER+ |
| GET | `/api/transactions/{id}/receipt` | レシートを取得 | CASHIER+ |

### Products

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/products` | 商品一覧（フィルタ・ページネーション対応） | CASHIER+ |
| POST | `/api/products` | 商品を作成 | MANAGER+ |
| PUT | `/api/products/{id}` | 商品を更新 | MANAGER+ |
| DELETE | `/api/products/{id}` | 商品を論理削除 | MANAGER+ |
| GET | `/api/products/search?barcode=` | バーコードで検索 | CASHIER+ |

### Categories

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/categories` | カテゴリ一覧（階層構造） | CASHIER+ |
| POST | `/api/categories` | カテゴリを作成 | MANAGER+ |
| PUT | `/api/categories/{id}` | カテゴリを更新 | MANAGER+ |
| DELETE | `/api/categories/{id}` | カテゴリを削除 | MANAGER+ |

### Tax Rates

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/tax-rates` | 税率一覧を取得 | CASHIER+ |
| POST | `/api/tax-rates` | 税率を作成 | MANAGER+ |
| PUT | `/api/tax-rates/{id}` | 税率を更新 | MANAGER+ |

### Discounts and Coupons

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/discounts` | 割引一覧を取得 | CASHIER+ |
| GET | `/api/coupons/validate?code=` | クーポンコードを検証 | CASHIER+ |

### Inventory

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/inventory?store_id=` | 在庫一覧を取得 | MANAGER+ |
| PUT | `/api/inventory/{id}/adjust` | 在庫数を調整 | MANAGER+ |
| POST | `/api/purchase-orders` | 発注を作成 | MANAGER+ |
| PUT | `/api/purchase-orders/{id}/receive` | 入荷を確認 | MANAGER+ |

### Analytics

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/analytics/daily` | 日次売上 | MANAGER+ |
| GET | `/api/analytics/products` | 商品別売上 | MANAGER+ |
| GET | `/api/analytics/hourly` | 時間帯別売上 | MANAGER+ |
| GET | `/api/analytics/summary` | 売上サマリー | MANAGER+ |
| GET | `/api/analytics/abc` | ABC 分析 | MANAGER+ |
| GET | `/api/analytics/gross-profit` | 粗利レポート | MANAGER+ |
| GET | `/api/analytics/forecast` | 売上予測 | MANAGER+ |

### Stores and Staff

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET | `/api/stores` | 店舗一覧を取得 | MANAGER+ |
| POST | `/api/stores` | 店舗を作成 | OWNER |
| GET | `/api/staff` | スタッフ一覧を取得 | MANAGER+ |
| POST | `/api/staff` | スタッフを作成 | OWNER |
| PUT | `/api/staff/{id}` | スタッフ情報を更新 | OWNER |

### Drawer and Settlement

| Method | Path | 説明 | Role |
|--------|------|------|------|
| POST | `/api/drawers/open` | キャッシュドロワーを開く | CASHIER+ |
| POST | `/api/drawers/close` | キャッシュドロワーを閉じる | CASHIER+ |
| GET | `/api/drawers/status` | ドロワー状態を取得 | CASHIER+ |
| POST | `/api/settlements` | 精算を作成 | MANAGER+ |
| GET | `/api/settlements/{id}` | 精算情報を取得 | MANAGER+ |

### Auth and Sync

| Method | Path | 説明 | Role |
|--------|------|------|------|
| POST | `/api/auth/pin-login` | スタッフ PIN ログイン | Terminal-authenticated |
| GET | `/api/sync/master` | マスターデータの差分を取得 | CASHIER+ |
| POST | `/api/sync/transactions` | オフライン取引を一括同期 | CASHIER+ |
| GET | `/api/health` | ヘルスチェック | None |

### その他のリソース

| Method | Path | 説明 | Role |
|--------|------|------|------|
| GET/POST | `/api/organizations` | 組織管理 | OWNER |
| GET/POST | `/api/system-settings` | システム設定 CRUD | OWNER |
| GET/POST | `/api/journals` | 電子ジャーナルエントリ | MANAGER+ |
| GET/POST | `/api/customers` | 顧客管理 | CASHIER+ |
| GET/POST | `/api/reservations` | 予約管理 | CASHIER+ |

## レスポンスフォーマット

### 成功時

```json
{
  "data": { ... },
  "meta": { "timestamp": "2026-03-18T12:00:00Z" }
}
```

### ページネーション時

```json
{
  "data": [ ... ],
  "meta": { "page": 0, "size": 20, "total": 150, "totalPages": 8 }
}
```

### エラー時

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Product name is required",
    "details": [...]
  }
}
```

## ページネーション

```
GET /api/products?page=0&size=20&sort=display_order,asc
```

## gRPC サービス（内部通信）

gRPC はクラスター内のサービス間通信に使用されます。Proto 定義は `proto/openpos/` にあります。

### サービス定義

| Package | Service | Proto File |
|---------|---------|-----------|
| `openpos.pos.v1` | `PosService` | `proto/openpos/pos/v1/pos.proto` |
| `openpos.product.v1` | `ProductService` | `proto/openpos/product/v1/product.proto` |
| `openpos.inventory.v1` | `InventoryService` | `proto/openpos/inventory/v1/inventory.proto` |
| `openpos.analytics.v1` | `AnalyticsService` | `proto/openpos/analytics/v1/analytics.proto` |
| `openpos.store.v1` | `StoreService` | `proto/openpos/store/v1/store.proto` |
| `openpos.store.v1` | `SystemSettingService` | `proto/openpos/store/v1/store.proto` |

### 主要な RPC

| Service | RPC | 説明 |
|---------|-----|------|
| PosService | `CreateTransaction` | 下書き取引を作成 |
| PosService | `AddTransactionItem` | 取引に明細を追加 |
| PosService | `FinalizeTransaction` | 決済を行い取引を完了 |
| PosService | `VoidTransaction` | 取引を無効化 |
| PosService | `SyncOfflineTransactions` | オフライン端末から一括同期 |
| PosService | `OpenDrawer` / `CloseDrawer` | キャッシュドロワー操作 |
| PosService | `CreateSettlement` | 精算を登録 |
| ProductService | `ListProducts` | フィルタ付き商品一覧 |
| ProductService | `GetProductByBarcode` | バーコード検索 |
| ProductService | `ValidateCoupon` | クーポン検証 |
| ProductService | `CreateTaxRate` / `ListTaxRates` | 税率管理 |
| InventoryService | `GetStock` / `ListStocks` | 在庫照会 |
| InventoryService | `AdjustStock` | 手動在庫調整 |
| InventoryService | `StartStocktake` / `CompleteStocktake` | 棚卸し |
| AnalyticsService | `GetDailySales` | 日次売上レポート |
| AnalyticsService | `GetAbcAnalysis` | ABC 商品分析 |
| AnalyticsService | `GetSalesForecast` | 移動平均による売上予測 |
| StoreService | `AuthenticateByPin` | スタッフ PIN 認証 |
| StoreService | `RegisterTerminal` | 端末登録 |
| SystemSettingService | `UpsertSystemSetting` | テナントレベルの設定 |

### gRPC Metadata

| Key | Value | 説明 |
|-----|-------|------|
| `authorization` | `Bearer {token}` | アクセストークン |
| `x-organization-id` | UUID | テナント識別子 |
| `x-staff-id` | UUID | 認証済みスタッフ |
| `x-role` | `OWNER` / `MANAGER` / `CASHIER` | スタッフロール |
| `x-request-id` | UUID | トレーシング用の相関 ID |

すべての RPC は 5 秒の deadline を使用します: `withDeadlineAfter(5, TimeUnit.SECONDS)`

### Proto ドキュメントの生成

Proto ファイルから HTML ドキュメントを生成するには以下を実行します。

```bash
buf generate --template buf.gen.doc.yaml
```

または `protoc-gen-doc` を使用します。

```bash
protoc --doc_out=docs/proto --doc_opt=html,index.html proto/openpos/**/*.proto
```

## RabbitMQ イベント

イベントは `openpos.events` topic exchange に発行されます。

| Routing Key | Publisher | Subscribers | 説明 |
|-------------|-----------|-------------|------|
| `sale.completed` | pos-service | inventory-service, analytics-service | 取引が確定 |
| `sale.voided` | pos-service | inventory-service, analytics-service | 取引が無効化 |
| `stock.low` | inventory-service | store-service | 在庫が閾値を下回った |
