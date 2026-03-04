# open-pos プロジェクト指示

## プロジェクト概要
汎用POSシステム（マルチテナント、オフライン対応）のモノリポ。

## 技術スタック
- Backend: Kotlin 2.3 / Quarkus 3.32 / GraalVM CE 21 / Gradle 9
- Frontend: React 19 / TypeScript / Vite 6 / Tailwind + shadcn/ui
- DB: PostgreSQL 17（スキーマ分離、Flyway migration）
- Cache: Redis 7（Lettuce、cache-aside パターン）
- MQ: RabbitMQ 4（SmallRye Reactive Messaging）
- Auth: ORY Hydra v2.2（OIDC/PKCE）
- gRPC: proto3 + buf toolchain

## ディレクトリ構成
- `proto/` — Protobuf 定義（buf workspace）
- `services/` — Quarkus マイクロサービス（6サービス）
- `apps/` — React フロントエンド（2アプリ）
- `packages/` — 共有 TypeScript パッケージ
- `infra/` — Docker Compose + init scripts
- `docs/` — 要件・設計ドキュメント

## コーディング規約

### 金額
- 全て BIGINT / int64 / number（銭単位: 10000 = 100円）
- 浮動小数点禁止

### マルチテナント
- 全テーブルに `organization_id` カラム
- Hibernate Filter で自動フィルタ
- gRPC metadata `x-organization-id` で伝播
- REST ヘッダー `X-Organization-Id`（api-gateway が注入）

### データベース
- スキーマ分離: store_schema, product_schema, pos_schema, inventory_schema, analytics_schema
- Flyway migration: `src/main/resources/db/migration/V{number}__{description}.sql`
- UUID 主キー

### gRPC
- proto 編集後は `buf lint` + `buf format -w`
- フィールド番号の再利用禁止（reserved で予約）

### テスト
- Backend: `@QuarkusTest` + `@InjectMock`
- Frontend: Vitest + React Testing Library

## ローカル開発
```bash
make up          # インフラ起動
make proto       # proto コード生成
./gradlew build  # バックエンドビルド
pnpm install     # フロントエンド依存インストール
pnpm dev:pos     # POS端末 dev server (port 5173)
pnpm dev:admin   # 管理画面 dev server (port 5174)
```

## Docker ポート
| サービス | ポート |
|---------|--------|
| PostgreSQL | 15432 |
| Redis | 16379 |
| RabbitMQ AMQP | 15672 |
| RabbitMQ UI | 15673 |
| Hydra Public | 14444 |
| Hydra Admin | 14445 |
| api-gateway | 8080 |
| pos-terminal | 5173 |
| admin-dashboard | 5174 |

## Issue 駆動開発
- ブランチ: `feature/#{issue番号}-短い説明`
- PR に `Closes #{issue番号}` を記載
- ラベル: `svc:*`, `app:*`, `type:*`, `P{0-3}:*`
