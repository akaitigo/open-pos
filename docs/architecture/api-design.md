# API設計

## REST API（api-gateway 公開）

### 認証

| ヘッダー | 説明 |
|---------|------|
| `Authorization: Bearer {token}` | アクセストークン（httpOnly cookie から自動付与） |
| `X-Organization-Id` | テナントID（gateway が JWT から注入、クライアント設定不要） |

### エンドポイント一覧

#### POS / 取引

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| POST | `/api/transactions` | 取引作成（確定） | CASHIER+ |
| GET | `/api/transactions/{id}` | 取引詳細 | CASHIER+ |
| GET | `/api/transactions` | 取引一覧 | MANAGER+ |
| POST | `/api/transactions/{id}/void` | VOID（当日取消） | MANAGER+ |
| POST | `/api/transactions/{id}/return` | 返品 | MANAGER+ |
| GET | `/api/transactions/{id}/receipt` | レシートPDF | CASHIER+ |

#### 商品

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| GET | `/api/products` | 商品一覧（フィルタ・ページング） | CASHIER+ |
| POST | `/api/products` | 商品作成 | MANAGER+ |
| PUT | `/api/products/{id}` | 商品更新 | MANAGER+ |
| DELETE | `/api/products/{id}` | 商品削除（論理） | MANAGER+ |
| GET | `/api/products/search?barcode=` | バーコード検索 | CASHIER+ |
| GET | `/api/categories` | カテゴリ一覧（階層） | CASHIER+ |
| POST | `/api/categories` | カテゴリ作成 | MANAGER+ |
| GET | `/api/discounts` | 割引一覧 | CASHIER+ |
| GET | `/api/coupons/validate?code=` | クーポン検証 | CASHIER+ |

#### 在庫

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| GET | `/api/inventory?store_id=` | 在庫一覧 | MANAGER+ |
| PUT | `/api/inventory/{id}/adjust` | 棚卸調整 | MANAGER+ |
| POST | `/api/purchase-orders` | 発注作成 | MANAGER+ |
| PUT | `/api/purchase-orders/{id}/receive` | 入荷確定 | MANAGER+ |

#### 分析

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| GET | `/api/analytics/daily` | 日次売上 | MANAGER+ |
| GET | `/api/analytics/products` | 商品別売上 | MANAGER+ |
| GET | `/api/analytics/hourly` | 時間帯別売上 | MANAGER+ |

#### 店舗・スタッフ

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| GET | `/api/stores` | 店舗一覧 | MANAGER+ |
| POST | `/api/stores` | 店舗作成 | OWNER |
| GET | `/api/staff` | スタッフ一覧 | MANAGER+ |
| POST | `/api/staff` | スタッフ追加 | OWNER |
| PUT | `/api/staff/{id}` | スタッフ更新 | OWNER |

#### 認証・同期

| メソッド | パス | 説明 | 権限 |
|--------|------|------|------|
| POST | `/api/auth/pin-login` | PINログイン | 端末認証済み |
| GET | `/api/sync/master` | マスタ差分取得 | CASHIER+ |
| POST | `/api/sync/transactions` | オフライン取引バッチ送信 | CASHIER+ |
| GET | `/api/health` | ヘルスチェック | 不要 |

### レスポンス形式

```json
// 成功
{ "data": { ... }, "meta": { "timestamp": "..." } }

// エラー
{ "error": { "code": "VALIDATION_ERROR", "message": "...", "details": [...] } }
```

### ページング

```
GET /api/products?page=0&size=20&sort=display_order,asc
```

レスポンス:
```json
{
  "data": [...],
  "meta": { "page": 0, "size": 20, "total": 150, "totalPages": 8 }
}
```

## gRPC（サービス間内部通信）

### サービス定義ファイル

```
proto/
  pos/v1/pos_service.proto
  product/v1/product_service.proto
  inventory/v1/inventory_service.proto
  analytics/v1/analytics_service.proto
  store/v1/store_service.proto
```

### 主要RPC

| サービス | RPC | 説明 |
|---------|-----|------|
| pos.v1.PosService | `CreateTransaction` | 取引確定 |
| pos.v1.PosService | `VoidTransaction` | VOID |
| product.v1.ProductService | `GetProduct` | 商品取得 |
| product.v1.ProductService | `ListProducts` | 商品一覧 |
| product.v1.ProductService | `ValidateCoupon` | クーポン検証 |
| inventory.v1.InventoryService | `GetStock` | 在庫取得 |
| store.v1.StoreService | `GetStaff` | スタッフ取得 |
| store.v1.StoreService | `VerifyPin` | PIN検証 |

### gRPC metadata 規約

```
authorization: Bearer {token}
x-organization-id: {uuid}
x-staff-id: {uuid}
x-role: OWNER|MANAGER|CASHIER
x-request-id: {uuid}
```

- Deadline: 全 RPC で `withDeadlineAfter(5, TimeUnit.SECONDS)` 必須
- エラーコード: `INVALID_ARGUMENT`, `NOT_FOUND`, `PERMISSION_DENIED`, `INTERNAL` 使用
