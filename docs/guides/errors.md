# エラーメッセージレジストリ

## REST API エラーレスポンス形式

```json
{
  "error": "NOT_FOUND",
  "message": "商品が見つかりません。",
  "details": null
}
```

## エラーコード一覧

### 認証・認可

| コード | HTTP | gRPC | メッセージ (ja) | メッセージ (en) |
|--------|------|------|----------------|----------------|
| `AUTH_REQUIRED` | 401 | UNAUTHENTICATED | 認証が必要です。 | Authentication required. |
| `AUTH_INVALID_TOKEN` | 401 | UNAUTHENTICATED | トークンが無効または期限切れです。 | Invalid or expired token. |
| `AUTH_FORBIDDEN` | 403 | PERMISSION_DENIED | この操作を行う権限がありません。 | Insufficient permissions. |
| `AUTH_PIN_INVALID` | 403 | PERMISSION_DENIED | PINが正しくありません。 | Invalid PIN. |
| `AUTH_ACCOUNT_LOCKED` | 403 | FAILED_PRECONDITION | アカウントがロックされています。 | Account is locked. |

### リソース

| コード | HTTP | gRPC | メッセージ (ja) | メッセージ (en) |
|--------|------|------|----------------|----------------|
| `PRODUCT_NOT_FOUND` | 404 | NOT_FOUND | 商品が見つかりません。 | Product not found. |
| `TRANSACTION_NOT_FOUND` | 404 | NOT_FOUND | 取引が見つかりません。 | Transaction not found. |
| `STORE_NOT_FOUND` | 404 | NOT_FOUND | 店舗が見つかりません。 | Store not found. |
| `STAFF_NOT_FOUND` | 404 | NOT_FOUND | スタッフが見つかりません。 | Staff not found. |
| `CUSTOMER_NOT_FOUND` | 404 | NOT_FOUND | 顧客が見つかりません。 | Customer not found. |

### ビジネスルール

| コード | HTTP | gRPC | メッセージ (ja) | メッセージ (en) |
|--------|------|------|----------------|----------------|
| `INSUFFICIENT_STOCK` | 409 | FAILED_PRECONDITION | 在庫が不足しています。 | Insufficient stock. |
| `INSUFFICIENT_POINTS` | 409 | FAILED_PRECONDITION | ポイントが不足しています。 | Insufficient points. |
| `INSUFFICIENT_BALANCE` | 409 | FAILED_PRECONDITION | 残高が不足しています。 | Insufficient balance. |
| `COUPON_EXPIRED` | 409 | FAILED_PRECONDITION | クーポンの有効期限が切れています。 | Coupon has expired. |
| `COUPON_MAX_USES` | 409 | FAILED_PRECONDITION | クーポンの利用上限に達しました。 | Coupon usage limit reached. |
| `PAYMENT_INSUFFICIENT` | 409 | FAILED_PRECONDITION | 支払い金額が不足しています。 | Payment amount is insufficient. |
| `TRANSACTION_NOT_DRAFT` | 409 | FAILED_PRECONDITION | 取引がドラフト状態ではありません。 | Transaction is not in draft status. |

### バリデーション

| コード | HTTP | gRPC | メッセージ (ja) | メッセージ (en) |
|--------|------|------|----------------|----------------|
| `INVALID_UUID` | 400 | INVALID_ARGUMENT | 不正なID形式です。 | Invalid ID format. |
| `INVALID_AMOUNT` | 400 | INVALID_ARGUMENT | 金額が不正です。 | Invalid amount. |
| `REQUIRED_FIELD` | 400 | INVALID_ARGUMENT | 必須フィールドが不足しています。 | Required field is missing. |
| `SAME_STORE_TRANSFER` | 400 | INVALID_ARGUMENT | 同一店舗間の在庫移動はできません。 | Cannot transfer stock to the same store. |

### テナント

| コード | HTTP | gRPC | メッセージ (ja) | メッセージ (en) |
|--------|------|------|----------------|----------------|
| `TENANT_REQUIRED` | 400 | INVALID_ARGUMENT | 組織IDが必要です。 | Organization ID is required. |
| `TENANT_INVALID` | 400 | INVALID_ARGUMENT | 不正な組織IDです。 | Invalid organization ID. |
| `TENANT_ACCESS_DENIED` | 403 | PERMISSION_DENIED | この組織へのアクセス権がありません。 | Access denied for this organization. |
