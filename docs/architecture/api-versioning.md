# API バージョニング戦略 (#208)

## 概要

open-pos REST API のバージョニング方針。後方互換性を維持しつつ、API の進化を可能にする。

## 戦略: URL パスプレフィックス方式

### 現行
```
/api/products
/api/stores
/api/transactions
```

### バージョニング適用後
```
/api/v1/products       (現行 API と同一)
/api/v1/stores
/api/v1/transactions
```

## 移行計画

### Phase 1: 既存 API のエイリアス追加（後方互換）

```
/api/products       → /api/v1/products  (両方利用可能)
/api/stores         → /api/v1/stores
```

- 既存の `/api/*` パスはそのまま維持
- `/api/v1/*` を新規に追加（同一ハンドラーへルーティング）
- クライアントの移行期間を設ける

### Phase 2: v2 API の導入（破壊的変更が必要な場合）

```
/api/v2/products    (新フォーマット)
/api/v1/products    (旧フォーマット、非推奨)
```

- v1 API は最低 6 ヶ月間維持
- 非推奨ヘッダー `Deprecation: true` を付与
- v2 レスポンスには `api-version: v2` ヘッダーを付与

## バージョニングルール

1. **パッチレベルの変更（バージョン変更不要）**
   - フィールドの追加（既存フィールドは削除しない）
   - オプショナルなクエリパラメータの追加
   - レスポンスへの追加フィールド

2. **マイナーレベルの変更（新バージョン推奨）**
   - フィールドの型変更
   - 必須パラメータの追加
   - エンドポイントの統合・リネーム

3. **メジャーレベルの変更（新バージョン必須）**
   - フィールドの削除
   - レスポンス構造の大幅変更
   - 認証方式の変更

## 実装方法

### api-gateway での JAX-RS 実装

```kotlin
// v1 パスを追加（既存 /api/products と共存）
@Path("/api/v1/products")
@Blocking
class ProductResourceV1 {
    @Inject
    lateinit var productResource: ProductResource

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> = productResource.list(page, pageSize)
}
```

### gRPC

gRPC のバージョニングは proto パッケージ名で管理済み:
```protobuf
package openpos.product.v1;
```

v2 が必要な場合は `openpos.product.v2` パッケージを追加する。

## レスポンスヘッダー

全レスポンスに以下のヘッダーを付与:
```
X-API-Version: v1
```

非推奨 API:
```
X-API-Version: v1
Deprecation: true
Sunset: 2027-01-01
Link: </api/v2/products>; rel="successor-version"
```

## クライアント対応

### フロントエンド（pos-terminal / admin-dashboard）

`api-client.ts` の BASE_URL を `/api/v1` に変更:
```typescript
const API_BASE = '/api/v1'
```

### 外部クライアント

API ドキュメント（OpenAPI）にバージョン情報を明記し、移行ガイドを提供する。
