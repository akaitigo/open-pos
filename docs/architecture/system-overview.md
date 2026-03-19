# システム全体像

## アーキテクチャ図

```
┌─────────────────────────────────────────┐
│           フロントエンド (PWA)             │
│  React / TypeScript / Vite               │
│  Dexie.js (IndexedDB) / Service Worker   │
└────────────────┬────────────────────────┘
                 │ HTTPS / REST
                 ▼
┌─────────────────────────────────────────┐
│           api-gateway                    │
│  Kotlin / Quarkus                        │
│  JWT検証 (ORY Hydra) / REST↔gRPC変換     │
│  レート制限 / CORS / テナントID注入        │
└──┬──────┬──────┬──────┬──────┬──────────┘
   │gRPC  │gRPC  │gRPC  │gRPC  │gRPC
   ▼      ▼      ▼      ▼      ▼
┌──────┐┌──────┐┌──────┐┌──────┐┌──────────┐
│ pos  ││prod  ││inven ││analy ││ store    │
│serv  ││serv  ││serv  ││serv  ││ serv     │
└──┬───┘└──┬───┘└──┬───┘└──┬───┘└──┬───────┘
   │       │       │       │       │
   └───────┴───────┴───────┴───────┘
                   │
        ┌──────────┼──────────┐
        ▼          ▼          ▼
   ┌─────────┐ ┌───────┐ ┌─────────┐
   │PostgreSQL│ │ Redis │ │RabbitMQ │
   │(Cloud SQL)│ │(Mem.)│ │         │
   └─────────┘ └───────┘ └─────────┘
```

## 技術スタック

| レイヤー | 技術 | 用途 |
|---------|------|------|
| Frontend | React 19 / TypeScript / Vite 7 | SPA/PWA |
| Frontend | Dexie.js | IndexedDB オフラインDB |
| Frontend | Service Worker | Background Sync |
| API Gateway | Kotlin / Quarkus / RESTEasy | REST エンドポイント |
| Backend | Kotlin / Quarkus / gRPC | マイクロサービス |
| Auth | ORY Hydra | OAuth2/OIDC プロバイダー |
| DB | PostgreSQL 17 / Cloud SQL | メインDB |
| DB Pool | pgBouncer | コネクションプール |
| Cache | Redis / Memorystore | キャッシュ |
| MQ | RabbitMQ | 非同期イベント |
| Infra | GCP Cloud Run | サービスホスト |
| Infra | GCP Cloud Storage | 画像ストレージ |
| Infra | GCP Cloud CDN | 静的配信 |

## サービス責務

| サービス | 責務 | 発行イベント | 購読イベント |
|---------|------|------------|------------|
| api-gateway | REST↔gRPC変換、認証検証、レート制限 | - | - |
| pos-service | 取引管理、税額計算、レシート生成 | `sale.completed`, `sale.voided`, `sale.returned` | - |
| product-service | 商品・カテゴリ・税率・割引・クーポン管理 | `product.updated`, `product.deleted` | - |
| inventory-service | 在庫管理、発注管理 | `stock.low` | `sale.completed`, `sale.voided`, `sale.returned` |
| analytics-service | 売上集計（日次・商品別・時間帯別） | - | `sale.completed`, `sale.voided`, `sale.returned` |
| store-service | 組織・店舗・端末・スタッフ管理 | - | `stock.low` |

## データ分離

- **スキーマ**: サービスごとに PostgreSQL スキーマを分離（`pos_schema`, `product_schema`, etc.）
- **テナント**: 全テーブルに `organization_id`、Hibernate Filter で自動フィルタ
- **サービス間通信**: gRPC（同期）または RabbitMQ（非同期）のみ（DB直接参照禁止）
