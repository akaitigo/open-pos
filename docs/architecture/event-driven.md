# イベント駆動設計

## RabbitMQ 設定

| 設定項目 | 値 |
|---------|-----|
| Exchange名 | `openpos.events` |
| Exchange種別 | `topic` |
| Durable | `true` |
| DLX（Dead Letter Exchange） | `openpos.events.dlx` |
| DLQ（Dead Letter Queue） | `openpos.events.dlq` |
| メッセージTTL | 7日（604800000ms） |

### ルーティングキー規約

```
{aggregate}.{action}
例: sale.completed, stock.low, product.updated
```

## イベント一覧

| イベント | 発行者 | 購読者 | 説明 |
|---------|--------|--------|------|
| `sale.completed` | pos-service | inventory-service, analytics-service | 取引確定 |
| `sale.voided` | pos-service | inventory-service, analytics-service | VOID |
| `sale.returned` | pos-service | inventory-service, analytics-service | 返品 |
| `stock.low` | inventory-service | store-service | 在庫アラート |
| `product.updated` | product-service | （将来: キャッシュ無効化） | 商品更新 |
| `product.deleted` | product-service | （将来: キャッシュ無効化） | 商品削除 |

## EventEnvelope 構造

全イベントは以下のエンベロープでラップする。

```json
{
  "event_id": "550e8400-e29b-41d4-a716-446655440000",
  "event_type": "sale.completed",
  "timestamp": "2024-01-01T12:00:00.000Z",
  "organization_id": "org-uuid",
  "version": 1,
  "payload": {
    // イベント固有データ
  }
}
```

### sale.completed payload

```json
{
  "transaction_id": "tx-uuid",
  "store_id": "store-uuid",
  "transaction_number": "STORE01-20240101-0001",
  "items": [
    {
      "product_id": "prod-uuid",
      "quantity": 2,
      "unit_price": 10000,
      "subtotal": 20000
    }
  ],
  "net_amount": 20000,
  "tax_amount": 1818
}
```

## 冪等性保証

### processed_events テーブル

```sql
CREATE TABLE processed_events (
  event_id     UUID PRIMARY KEY,
  event_type   VARCHAR(100) NOT NULL,
  processed_at TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

- 各 consumer サービスが独自の `processed_events` テーブルを持つ
- メッセージ受信時に `event_id` を INSERT（重複時は UNIQUE 制約でエラー）
- エラー発生時は `basicNack`（requeue=false）→ DLQ へルーティング

## リトライ・DLQ

```
Consumer が例外 → basicNack(requeue=false)
→ DLX (openpos.events.dlx) へルーティング
→ DLQ (openpos.events.dlq) に蓄積
→ 手動確認・再処理 or 廃棄
```

### アラート
- DLQ の深度が 10 件を超えたらアラート（Cloud Monitoring）

## Consumer 実装規約（Kotlin/Quarkus）

```kotlin
@ApplicationScoped
class SaleCompletedConsumer {

    @Incoming("sale-completed")
    @Transactional
    fun consume(envelope: EventEnvelope) {
        // 1. 冪等性チェック
        if (processedEventsRepository.exists(envelope.eventId)) return

        // 2. ビジネスロジック実行
        inventoryService.deductStock(envelope.payload)

        // 3. 処理済み記録
        processedEventsRepository.save(envelope.eventId, envelope.eventType)

        // 自動 ack（@Incoming が管理）
    }
}
```

- 処理完了後に ack（処理前の ack 禁止）
- トランザクション内で冪等性チェックと処理を原子的に実行
