# open-pos プロジェクト指示

## プロジェクト概要
汎用POSシステム（マルチテナント、オフライン対応）のモノリポ。

## v1.0 リリースロードマップ
**必ず [`docs/plans/v1-release-roadmap.md`](../../docs/plans/v1-release-roadmap.md) を参照してから作業すること。**
- GitHub マイルストーン: [v1.0.0 — OSS Public Release](https://github.com/akaitigo/open-pos/milestone/18) (65 issues)
- Phase 0（ブロッカー解除）→ Phase 1（セキュリティ）→ Phase 2（ビジネスロジック）→ Phase 3（耐障害性）→ Phase 4（ドキュメント）→ Phase 5（品質）
- イシュー番号に `gh issue view #XXX` で詳細を確認してから着手
- 既存PR: #313 (Docker build), #314 (テスト), #316 (Flyway) が未マージ

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

### テスト（必須 — 全機能実装に適用）

**全ての機能実装には、単体テスト・機能テスト・結合テスト・E2Eテストを必ずコードとして含める。**
詳細戦略: [`docs/guides/testing.md`](docs/guides/testing.md) / リサーチ: `docs/research/testing-*.md`

| レベル | Backend | Frontend | 場所 |
|-------|---------|----------|------|
| 単体テスト | JUnit 5 + `@InjectMock` | Vitest + RTL | `src/test/kotlin/` / `src/**/*.test.ts(x)` |
| 機能テスト | JUnit 5 + `@QuarkusTest` | Vitest + RTL | `**/functional/` / `*.functional.test.ts(x)` |
| 結合テスト | `@QuarkusTest` + Testcontainers | — | `**/integration/` |
| E2Eテスト | Playwright | Playwright | `e2e/` |

**ルール**:
- AAA パターン（Arrange-Act-Assert）で記述
- 1テスト = 1検証意図
- 外部依存は `@InjectMock` / `vi.mock` / Testcontainers で隔離
- 金額計算は境界値テスト必須（0円、1円、端数、最大値、税率切替）
- E2E は `data-testid` + Page Object Model。手動 sleep 禁止
- 新規コードの行カバレッジ 80%+ 必須
- Red → Green: 実装前にテストが失敗することを確認

**実行コマンド**:
```bash
./gradlew :services:{name}:test          # Backend 単体+機能
./gradlew :services:{name}:test -Dquarkus.test.profile=integration  # 結合
pnpm --filter {app} test                 # Frontend 単体+機能
pnpm --filter e2e test                   # E2E
```

## アーキテクチャ制約

### Backend レイヤー（依存方向: 上→下のみ）
```
Proto 定義 → Config/Filter → Entity → Repository → Service → gRPC Handler
```

**禁止**:
- Entity が Service を参照
- gRPC Handler が Repository を直接呼び出し
- Service 間の直接依存（RabbitMQ イベント経由のみ）

### マルチテナント実装パターン

gRPC Interceptor で metadata → Context → Hibernate Filter の順に伝播:
```kotlin
// 1. OrganizationIdInterceptor: metadata から UUID を抽出
val orgIdStr = headers.get(Metadata.Key.of("x-organization-id", ASCII_STRING_MARSHALLER))
val ctx = Context.current().withValue(ORGANIZATION_ID_CTX_KEY, UUID.fromString(orgIdStr))

// 2. GrpcTenantHelper: Context から取得して OrganizationIdHolder に設定
OrganizationIdHolder.set(ORGANIZATION_ID_CTX_KEY.get())

// 3. TenantFilterService: Hibernate Filter を有効化
session.enableFilter("organizationFilter").setParameter("organizationId", orgId)
```

テスト時は `OrganizationIdHolder.set(testOrgId)` で直接設定。

## コーディング規約詳細

→ 詳細は `.claude/rules/` を参照:
- **Kotlin/Quarkus**: `rules/kotlin.md`（Null安全性、DB、CDI、RabbitMQ、Redis）
- **TypeScript/React**: `rules/typescript.md`（型安全性、React、認証、セキュリティ）
- **Proto/gRPC**: `rules/proto.md`（proto3 構文、buf ツールチェーン、gRPC 設計）
- **フロントエンド**: `rules/frontend.md`（コンポーネント構造、金額表示、オフライン）
- **マイクロサービス**: `rules/service.md`（パッケージ構造、テナント分離、RabbitMQ、キャッシュ）

## トラブルシューティング

### CI
- `setup-java`: `distribution: temurin` を使用（`graalce` は非対応）
- `buf breaking`: ローカル git 参照 + `git fetch origin main:main` が必須
- `pnpm`: `package.json` に `"packageManager": "pnpm@10.30.3"` が必須

### テスト
- **ヘルスチェック**: HTTP GET `/q/health` は gRPC サービスで 404 → CDI inject `SmallRyeHealthReporter` を使用
- **SmallRyeHealth**: `.status` は存在しない → `.isDown.not()` で判定
- **H2 テスト DB**: `testImplementation("io.quarkus:quarkus-jdbc-h2")`

### Docker
- **サービス間通信**: `localhost:8080` ではなく Docker ネットワーク内のサービス名を使用
- **Hydra マイグレーション**: `hydra-migrate` init サービスで自動実行

### GitHub CLI
- `GITHUB_TOKEN` に Fine-grained PAT がセットされると `gh` CLI の OAuth トークンを上書きする
- 対策: `unset GITHUB_TOKEN` してから `gh` コマンド実行

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

## PR マージルール（厳守）

### 絶対禁止
- **`gh pr merge --admin` は絶対に使わない** — CIバイパスは禁止
- **`git push --force` to main は絶対に使わない**
- CI が失敗している PR をマージしない

### 必須手順
1. ブランチ作成 → コミット → push
2. `gh pr create` で PR 作成
3. **`gh pr merge --squash --auto`** でオートマージ設定
4. CI が全て pass したら自動的にマージされる
5. CI が失敗したら **原因を調査・修正して再 push**（リトライではなく修正）

### CI 失敗時の対応
- E2E テスト失敗 → ログを確認し、コード起因か flaky かを判断
- flaky の場合 → `gh run rerun {run_id} --failed` でリトライ（1回のみ）
- コード起因の場合 → ブランチ上で修正して push
- **絶対に `--admin` で強制マージしない**
