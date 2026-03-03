# 在庫管理 機能要件

## 在庫CRUD

### 在庫レコード

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 店舗ID |
| product_id | UUID | 商品ID |
| quantity | INT | 現在庫数 |
| alert_threshold | INT | 低在庫アラート閾値 |
| updated_at | TIMESTAMP | 最終更新日時 |

- `(store_id, product_id)` でユニーク制約
- `quantity` は楽観的ロック（`@Version`）で更新

## 在庫移動種別

| 種別 | 説明 | 数量変化 |
|------|------|---------|
| `SALE` | POS販売 | 減少 |
| `RETURN` | 返品 | 増加 |
| `RECEIPT` | 入荷 | 増加 |
| `ADJUSTMENT` | 棚卸調整 | 増減 |
| `TRANSFER` | 店舗間移動 | 移動元: 減少 / 移動先: 増加 |

### 在庫移動ログ（stock_movements）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| stock_id | UUID | 在庫レコードID |
| movement_type | ENUM | 上記種別 |
| quantity_delta | INT | 変化量（負数で減少） |
| reference_id | UUID | 取引ID等の参照 |
| note | TEXT | 備考 |
| created_at | TIMESTAMP | 記録日時 |

## イベント連携（自動在庫減算）

```
pos-service → [sale.completed] → RabbitMQ → inventory-service
```

1. `sale.completed` イベントを inventory-service が購読
2. 取引明細から `store_id` + `product_id` + `quantity` を抽出
3. `stocks` の `quantity` を減算
4. `stock_movements` に `SALE` レコードを挿入
5. 減算後に `alert_threshold` を下回れば `stock.low` イベントを発行

### 冪等性保証
- `processed_events` テーブルで `event_id` の重複チェック
- 同一 `event_id` は処理スキップ

## 在庫アラート

```
inventory-service → [stock.low] → RabbitMQ → store-service（通知）
```

- アラートイベントペイロード: `store_id`, `product_id`, `current_quantity`, `threshold`
- 通知先: 店長・オーナーへのダッシュボード通知

## 発注管理

### 発注ステータスフロー

```
DRAFT → ORDERED → RECEIVED
         ↓
       CANCELLED（ORDERED前のみ）
```

### 発注テーブル（purchase_orders）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 発注店舗 |
| status | ENUM | `DRAFT`/`ORDERED`/`RECEIVED`/`CANCELLED` |
| ordered_at | TIMESTAMP | 発注日時 |
| received_at | TIMESTAMP | 入荷日時 |
| note | TEXT | 備考 |

- 入荷確定（RECEIVED）時に `stock_movements(RECEIPT)` を自動生成

## 受け入れ条件

- [ ] `sale.completed` 受信から在庫減算まで非同期で実行される
- [ ] 同一イベントを2回受信しても在庫が2回減算されない
- [ ] 在庫がマイナスになった場合、`stock.low` イベントが発行される
- [ ] TRANSFER で移動元・移動先の合計在庫が変化しない
