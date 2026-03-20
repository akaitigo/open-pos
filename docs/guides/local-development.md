# ローカル開発モードガイド

open-pos には 2 つのローカル開発モードがあります。目的に応じて使い分けてください。

## モード比較

| | quarkusDev モード | local-demo / docker-demo モード |
| --- | --- | --- |
| **概要** | 個別サービスを Quarkus Dev Mode で起動 | 全スタック（インフラ + バックエンド + シードデータ）を一括起動 |
| **コマンド** | `make dev-{service}` | `make local-demo` / `make docker-demo` |
| **ホットリロード** | あり（コード変更を即時反映） | なし（再ビルドが必要） |
| **対象サービス** | 1つ（開発中のサービス） | 全 supported backend（5サービス） |
| **デモデータ** | 手動シード | 自動シード |
| **主な用途** | 単一サービスの機能開発・デバッグ | 結合テスト、E2E テスト、デモ |
| **起動速度** | 速い（単一サービス） | 遅い（全サービスビルド + 起動） |

## quarkusDev モード

### 概要

Quarkus Dev Mode（`quarkusDev`）は、個別のバックエンドサービスを開発するためのモードです。コード変更時にホットリロードが効くため、迅速なイテレーションが可能です。

### いつ使うか

- 特定のサービスの機能を開発しているとき
- gRPC ハンドラーやビジネスロジックのデバッグ
- 単体テスト・機能テストの実行
- Flyway マイグレーションの動作確認

### 前提条件

- Java 21（GraalVM CE 推奨）
- Docker（インフラ起動用）
- `make doctor` が成功すること

### 起動手順

```bash
# 1. インフラを起動（PostgreSQL, Redis, RabbitMQ, Hydra）
make up

# 2. 開発対象のサービスを quarkusDev で起動
make dev-product     # product-service
make dev-store       # store-service
make dev-pos         # pos-service
make dev-inventory   # inventory-service
make dev-analytics   # analytics-service
make dev-gateway     # api-gateway

# 全バックエンドサービスをバックグラウンドで起動（結合確認時）
make dev-backend
```

各サービスの Quarkus Dev Mode ポート:

| サービス | gRPC ポート | HTTP ポート（ヘルスチェック） |
| --- | --- | --- |
| product-service | 9001 | 8081 |
| store-service | 9002 | 8082 |
| pos-service | 9003 | 8083 |
| inventory-service | 9004 | 8084 |
| analytics-service | 9005 | 8085 |
| api-gateway | -- | 8080 |

### quarkusDev の機能

#### ホットリロード

Kotlin ソースを変更すると、次のリクエスト（gRPC / HTTP）で自動的に再コンパイル・リロードされます。サービスの再起動は不要です。

#### Dev UI

Quarkus Dev Mode では Dev UI（`http://localhost:{port}/q/dev-ui`）が利用できます。

- CDI Bean の一覧
- 設定値の確認・変更
- Flyway マイグレーション状態
- ヘルスチェック

#### 継続的テスト

Dev Mode 起動中に `r` キーを押すとテストを再実行できます。コード変更時に影響のあるテストだけ自動実行されます。

### よくある問題

#### ポート競合

`make local-demo` や `make docker-demo` で起動したバックエンドが残っている場合、ポートが競合します。先に停止してください:

```bash
make local-down
make docker-down-core
```

#### データベースマイグレーションエラー

初回起動時や新しいマイグレーションを追加した場合にエラーが出ることがあります:

```bash
# PostgreSQL のデータをリセット
make reset
```

#### 依存サービスが未起動

api-gateway は他のサービスに gRPC で接続します。テスト対象のエンドポイントに必要なサービスが起動していることを確認してください。

---

## local-demo モード

### 概要

`make local-demo` は、インフラを Docker で起動し、バックエンドサービスをホスト上でビルド済み JAR として実行するモードです。日常的な開発での推奨パスです。

### いつ使うか

- フロントエンドとバックエンドの結合動作を確認したいとき
- デモデータを使った動作確認
- フロントエンド開発（POS 端末 / 管理画面）
- `make verify-full`（Playwright E2E テスト含む）を実行する前

### 前提条件

- Java 21（GraalVM CE 推奨）
- Node.js 22+ / pnpm 10+
- Docker & Docker Compose
- `make doctor` が成功すること
- `pnpm install` 実行済み

### 起動手順

```bash
# 一発で全て起動（推奨）
make local-demo

# フロントエンド開発サーバーを起動
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

`make local-demo` が行うこと:

1. `make up` -- Docker でインフラ（PostgreSQL, Redis, RabbitMQ, Hydra）を起動
2. `make local-up` -- 全 supported backend をビルドしてホスト上で起動
3. `make local-seed` -- デモデータのシード + `demo-config.json` 生成
4. `make local-smoke` -- API gateway 経由のスモークテスト

### 操作コマンド

```bash
# コード変更後の再起動（再ビルドあり）
make local-down
make local-up

# コード変更後の再起動（再ビルドなし、設定変更のみの場合）
make local-up-fast

# バックエンドの停止
make local-down

# デモデータの再投入
make local-seed
make local-smoke

# ログ確認
make logs-pos       # pos-service のログ
ls .local/logs/     # 各サービスのログファイル
```

### ログと PID

ホスト実行モードのログと PID は以下に出力されます:

- `.local/logs/{service}.log` -- 各サービスのログ
- `.local/pids/{service}.pid` -- 各サービスのプロセス ID

### よくある問題

#### ビルドが失敗する

Gradle のキャッシュが壊れている場合があります:

```bash
./gradlew clean
make local-demo
```

#### バックエンドプロセスが残留する

`make local-down` が正常に終了しなかった場合、プロセスが残ることがあります:

```bash
# PID ファイルを確認
cat .local/pids/*.pid

# 残留プロセスを手動で停止
kill $(cat .local/pids/*.pid) 2>/dev/null
make local-down
```

---

## docker-demo モード

### 概要

`make docker-demo` は、バックエンドサービスも Docker コンテナ内で実行するモードです。CI/CD パイプラインやリリースに近い環境で動作確認したい場合に使います。

### いつ使うか

- CI の挙動を手元で再現したいとき
- コンテナイメージのビルド確認
- リリース前の最終検証
- Dockerfile の変更テスト

### 前提条件

- Docker & Docker Compose
- 十分なディスク容量（コンテナイメージのビルドに必要）
- `pnpm install` 実行済み（フロントエンド開発サーバー用）

> [!NOTE]
> docker-demo モードでは Java や Gradle のホストインストールは不要です。ビルドはコンテナ内で行われます。

### 起動手順

```bash
# 一発で全て起動
make docker-demo

# フロントエンド開発サーバーを起動
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

`make docker-demo` が行うこと:

1. `make docker-build-core` -- supported backend の Docker イメージをビルド
2. `make docker-up-core` -- インフラ + バックエンドコンテナを起動
3. `make docker-smoke` -- デモデータのシード + スモークテスト

### 操作コマンド

```bash
# コンテナイメージの再ビルド
make docker-build-core

# バックエンドコンテナの起動/停止
make docker-up-core
make docker-down-core

# 全コンテナのログ
make logs

# 全コンテナの状態確認
docker compose -f infra/compose.yml ps
```

### よくある問題

#### イメージビルドが遅い

初回ビルドは Gradle の依存関係ダウンロードを含むため時間がかかります。2回目以降は Docker のレイヤーキャッシュが効きます。

ビルド速度を改善するには:

```bash
# 特定サービスだけビルド
docker compose -f infra/compose.yml build product-service
```

#### コンテナが unhealthy

```bash
# ログを確認
docker compose -f infra/compose.yml logs postgres
docker compose -f infra/compose.yml logs api-gateway

# 完全リセット
docker compose -f infra/compose.yml down -v
make docker-demo
```

---

## モード間の切り替え

同時に使えるバックエンドモードは 1 つだけです。切り替え時は現在のモードを必ず停止してください。

### local-demo から docker-demo へ

```bash
make local-down
make docker-demo
```

### docker-demo から local-demo へ

```bash
make docker-down-core
make local-demo
```

### quarkusDev から local-demo / docker-demo へ

quarkusDev のプロセスを `Ctrl+C` で停止してから、目的のモードを起動してください。

### 完全リセット

どのモードからでも初期状態に戻せます:

```bash
make local-down
make docker-down-core
make down
make reset
```

`make reset` は PostgreSQL のデータボリュームを再作成し、最後に検出されたバックエンドモードで再起動・再シードします。

---

## フロントエンド開発

フロントエンドの開発サーバーは、どのバックエンドモードでも同じコマンドで起動できます:

```bash
pnpm dev:pos     # POS 端末 → http://localhost:5173
pnpm dev:admin   # 管理画面 → http://localhost:5174
```

### ランタイム設定ファイル

`make local-seed`（`make local-demo` / `make docker-demo` に含まれる）が以下のファイルを生成します:

| ファイル | 用途 |
| --- | --- |
| `apps/pos-terminal/.env.development.local` | Vite 環境変数 |
| `apps/admin-dashboard/.env.development.local` | Vite 環境変数 |
| `apps/pos-terminal/public/demo-config.json` | ランタイムデモ設定 |
| `apps/admin-dashboard/public/demo-config.json` | ランタイムデモ設定 |

`demo-config.json` はブラウザからランタイムに読み込まれるため、再シード後はブラウザをリロードするだけで最新データが反映されます（dev server の再起動は不要）。

### シードデータ

シードされるデモデータは冪等で、以下が作成されます:

| データ | 内容 |
| --- | --- |
| 組織 | `テスト株式会社`（`T1234567890123`） |
| 店舗 | `渋谷店`、`新宿店` |
| 端末 | 各店舗に 2 台 |
| スタッフ | Owner / Manager / Cashier（PIN: `1234` / `2345` / `3456`） |
| 商品 | 4 カテゴリ、40 商品 |
| 在庫 | 各商品 100 個 |
| 取引 | 10 件（COMPLETED 3 / VOIDED 1 / DRAFT 6） |

---

## ヘルスチェックとデバッグ用エンドポイント

| サービス | URL | 備考 |
| --- | --- | --- |
| API Gateway | http://localhost:8080/api/health | メインのスモークテスト対象 |
| Product Service | http://localhost:8081/health | ホスト実行モード |
| Store Service | http://localhost:8082/health | ホスト実行モード |
| POS Service | http://localhost:8083/health | ホスト実行モード |
| PostgreSQL | localhost:15432/openpos | ユーザー: `openpos` / パスワード: `openpos_dev` |
| Redis | localhost:16379 | 認証なし |
| RabbitMQ UI | http://localhost:15673 | ユーザー: `openpos` / パスワード: `openpos_dev` |
| Hydra Public | http://localhost:14444 | OAuth/OIDC 公開エンドポイント |
| Hydra Admin | http://localhost:14445 | Hydra 管理 API |
| pgAdmin | http://localhost:15080 | `make up-dev` で起動 |
| Redis Commander | http://localhost:18081 | `make up-dev` で起動 |

```bash
# Compose 状態確認
docker compose -f infra/compose.yml ps

# API ヘルスチェック
curl -s http://localhost:8080/api/health

# gRPC ヘルスチェック（grpcurl が必要）
make grpc-test
```

---

## 関連ドキュメント

- [セットアップガイド](setup.md) -- 初期セットアップ手順
- [ローカル開発 Runbook](../runbook/local-dev.md) -- 運用向け詳細手順
- [テストガイド](testing.md) -- テスト戦略と実行方法
- [コントリビューションガイド](contributing.md) -- PR 作成ワークフロー
