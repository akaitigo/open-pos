# 売上分析 機能要件

## 集計方式

analytics-service は RabbitMQ イベントを購読して集計テーブルを非同期更新する。
OLAP クエリは analytics_schema に対して実行し、本番トランザクションDBに影響を与えない。

## イベント購読

| イベント | 処理 |
|---------|------|
| `sale.completed` | 各集計テーブルへ加算 |
| `sale.voided` | 各集計テーブルから減算 |
| `sale.returned` | 返品分を差し引き |

冪等性: `processed_events` テーブルで `event_id` 重複排除。

## 日次売上集計（daily_sales）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 店舗ID |
| date | DATE | 集計日 |
| gross_amount | BIGINT | 総売上（銭単位） |
| discount_amount | BIGINT | 割引合計 |
| net_amount | BIGINT | 純売上（gross - discount） |
| tax_amount | BIGINT | 税額合計 |
| transaction_count | INT | 取引件数 |
| cash_amount | BIGINT | 現金支払合計 |
| card_amount | BIGINT | カード支払合計 |
| qr_amount | BIGINT | QR支払合計 |

- `(store_id, date)` でユニーク制約
- アップサート（INSERT ... ON CONFLICT DO UPDATE）

## 商品別売上集計（product_sales）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 店舗ID |
| product_id | UUID | 商品ID |
| date | DATE | 集計日 |
| quantity_sold | INT | 販売数量 |
| gross_amount | BIGINT | 販売金額合計 |
| discount_amount | BIGINT | 割引合計 |
| net_amount | BIGINT | 純売上 |

- `(store_id, product_id, date)` でユニーク制約

## 時間帯別売上（hourly_sales）

| フィールド | 型 | 説明 |
|-----------|-----|------|
| id | UUID | 主キー |
| organization_id | UUID | テナントID |
| store_id | UUID | 店舗ID |
| date | DATE | 集計日 |
| hour | SMALLINT | 時間帯（0-23） |
| transaction_count | INT | 取引件数 |
| net_amount | BIGINT | 純売上 |

- ピーク時間帯分析に使用
- `(store_id, date, hour)` でユニーク制約

## APIエンドポイント（analytics-service → api-gateway）

| エンドポイント | 説明 |
|--------------|------|
| `GET /api/analytics/daily?store_id=&from=&to=` | 日次売上一覧 |
| `GET /api/analytics/products?store_id=&date=` | 商品別売上 |
| `GET /api/analytics/hourly?store_id=&date=` | 時間帯別売上 |

- 全エンドポイントに `organization_id` フィルタ強制適用
- 権限: OWNER / MANAGER のみアクセス可

## 受け入れ条件

- [ ] `sale.completed` 受信から集計テーブル更新まで5秒以内
- [ ] VOIDされた取引は集計から除外される
- [ ] 異なる `store_id` のデータが混入しない
- [ ] 日次集計の `net_amount` が `gross_amount - discount_amount` と一致する
