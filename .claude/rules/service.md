---
globs:
  - "services/**/*.kt"
  - "services/**/*.properties"
---

# サービス開発ルール

## パッケージ構造
```
com.openpos.{service}/
├── entity/       # JPA エンティティ
├── repository/   # Panache リポジトリ
├── service/      # ビジネスロジック
├── grpc/         # gRPC サーバー実装
├── event/        # RabbitMQ イベント処理
└── config/       # 設定・フィルター
```

## テナント分離
- 全エンティティに `organizationId` フィールド
- `@Filter` + `@FilterDef` で Hibernate フィルター設定
- gRPC Interceptor で metadata から `x-organization-id` を取得

## RabbitMQ
- `@Incoming` / `@Outgoing` アノテーション使用
- 冪等性: `processed_events` テーブルで event_id チェック
- 失敗時: nack → DLQ

## Redis キャッシュ
- キー形式: `openpos:{service}:{orgId}:{entity}:{id}` — **orgId 必須**（テナント間キャッシュ漏えい防止。セキュリティ評価 P0-1 で全実装改修済み）
- キー構築はキャッシュサービスのヘルパーを経由し orgId の付与漏れを防ぐ（例: api-gateway の `RedisCacheService.tenantKey(orgId, ...)`）
- 全キーに TTL 設定（デフォルト 1h）
- cache-aside パターン遵守
