# オフライン対応 機能要件

## 概要

ネットワーク断絶時も POS 会計を継続できるよう、フロントエンドに Dexie.js（IndexedDB ラッパー）を使用したローカルDBを持つ。

## ローカルDBストア（Dexie.js）

| ストア名 | 用途 | インデックス |
|---------|------|------------|
| `products` | 商品マスタキャッシュ | `id, barcode, sku, categoryId` |
| `categories` | カテゴリキャッシュ | `id, parentId` |
| `tax_rates` | 税率キャッシュ | `id` |
| `discounts` | 割引マスタキャッシュ | `id` |
| `staff` | スタッフキャッシュ | `id, storeId` |
| `store_settings` | 店舗設定キャッシュ | `storeId` |
| `pending_transactions` | 未送信取引キュー | `clientId, createdAt, syncStatus` |
| `sync_metadata` | 最終同期タイムスタンプ | `entityType` |

## マスタデータ同期

### 差分同期API

```
GET /api/sync/master?since={ISO8601_timestamp}&store_id={storeId}
```

レスポンス:
```json
{
  "products": [...],
  "categories": [...],
  "tax_rates": [...],
  "discounts": [...],
  "staff": [...],
  "store_settings": {...},
  "sync_timestamp": "2024-01-01T00:00:00Z"
}
```

- `since` 以降に更新されたレコードのみ返す
- 削除レコードは `deleted: true` フラグで返す（論理削除）
- Service Worker で5分間隔にバックグラウンド同期

### 初回同期
- アプリ起動時にフル同期（`since` なし）
- 同期完了まで「データ準備中」表示

## 取引同期（Background Sync）

### フロー

```
オフライン取引 → IndexedDB(pending_transactions) に保存
ネットワーク復帰 → Service Worker の sync イベント発火
→ POST /api/sync/transactions（バッチ）
→ サーバー処理（冪等性チェック）
→ 成功: IndexedDB から削除
→ 失敗: リトライ（最大5回）またはエラー通知
```

### バッチ送信API

```
POST /api/sync/transactions
Body: { "transactions": [ {..., clientId: "uuid"}, ... ] }
```

- `clientId` で重複排除（冪等性保証）
- バッチサイズ: 最大50件

### syncStatus

| 値 | 説明 |
|----|------|
| `pending` | 未送信 |
| `syncing` | 送信中 |
| `failed` | 送信失敗（リトライ待ち） |
| `completed` | 送信済み |

## コンフリクト解決ルール

| シナリオ | 解決策 |
|---------|-------|
| 価格変更（オフライン中） | 取引時の価格スナップショットを優先（変更後価格は使用しない） |
| 在庫マイナス | サーバー側でマイナスを許容し `stock.low` イベントを発行 |
| スタッフ削除（オフライン中） | 取引を受理し、監査ログに記録 |
| クーポン重複利用 | サーバー側で利用回数チェック、超過分は拒否しエラー返却 |

## ネットワーク状態検知

- `navigator.onLine` + `fetch` ヘルスチェック（`/api/health`）で判定
- オフライン時はヘッダーバーにオフラインバナー表示
- 復帰時はトースト通知 + 自動同期開始

## 受け入れ条件

- [ ] オフライン中に取引を作成できる
- [ ] オンライン復帰後に自動で取引が同期される
- [ ] 同一 `clientId` の取引がサーバーに2件作成されない
- [ ] マスタ同期は差分のみ取得する（フル同期はアプリ起動時のみ）
- [ ] 未同期件数をUIで確認できる
