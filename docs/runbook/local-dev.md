# ローカル開発 Runbook

この Runbook では、`open-pos` の日常的な開発フローについて説明します。

## 推奨モード

### ホスト実行のローカルバックエンド

通常の開発にはこちらを使用してください。インフラは Docker で動作し、バックエンドはホスト上でビルド済みの Quarkus jar として実行されます。

```bash
make local-demo
pnpm dev:admin   # http://localhost:5174
pnpm dev:pos     # http://localhost:5173
```

`make local-demo` は以下のすべてを実行します:

- Docker Compose でインフラを起動
- `product-service`、`store-service`、`pos-service`、`inventory-service`、`api-gateway` をホスト上でビルド・起動
- 冪等なデモデータを投入
- フロントエンドのランタイム設定ファイルを書き出し
- API スモークチェックを実行

### コンテナ実行のローカルバックエンド

コンテナ化された CI/リリースパスにより近い環境で動作確認したい場合にこちらを使用してください。

```bash
make docker-demo
pnpm dev:admin
pnpm dev:pos
```

ホスト実行モードから切り替える場合は、先にローカルバックエンドを停止してください。

```bash
make local-down
make docker-up-core
```

## よく使うコマンド

### インフラのみ

```bash
make up       # PostgreSQL, Redis, RabbitMQ, Hydra
make up-dev   # インフラ + pgAdmin + Redis Commander
make down
make logs     # Docker Compose ログ
```

### ホスト実行バックエンド

```bash
make local-up       # リビルドして起動
make local-up-fast  # リビルドせずに起動
make local-down
make logs-pos       # ホスト実行モード中に .local/logs/pos-service.log を tail
```

ホスト実行バックエンドのログと PID は以下に書き出されます:

- `.local/logs/`
- `.local/pids/`

### コンテナ実行バックエンド

```bash
make docker-build        # 全バックエンドイメージ
make docker-build-core
make docker-up-core
make docker-down-core
```

### シードとスモークテスト

```bash
make local-seed
make local-smoke
make docker-smoke
make grpc-test
make db-backup
make db-restore FILE=.local/backups/openpos-YYYYmmdd-HHMMSS.sql
make reset
```

`make reset` は PostgreSQL のデータボリュームを再作成し、最後に検出されたバックエンドモードを復元した上で、デモデータを再投入します。バックエンドモードが実行されていなかった場合は、ホスト実行のローカルバックエンドを起動してから再投入します。

投入されるデモデータ:

- `テスト株式会社` (`T1234567890123`)
- 店舗: `渋谷店` と `新宿店`
- 各店舗に owner / manager / cashier スタッフ (`1234` / `2345` / `3456`)
- 4 カテゴリ、40 商品、在庫は `100` に正規化
- 10 件のサンプル取引 (`COMPLETED 3 / VOIDED 1 / DRAFT 6`)

### フロントエンド開発サーバー

```bash
pnpm dev:admin
pnpm dev:pos
```

### 検証

```bash
make doctor
make verify
pnpm e2e:install
make verify-full
```

`make doctor` は `grpcurl` が未インストールの場合に警告を出します。`make grpc-test` が `grpcurl` に依存しているためです。

## ランタイム設定ファイル

`scripts/seed.sh` は環境ファイルとランタイム設定ファイルの両方を書き出します:

- `apps/admin-dashboard/.env.development.local`
- `apps/pos-terminal/.env.development.local`
- `apps/admin-dashboard/public/demo-config.json`
- `apps/pos-terminal/public/demo-config.json`

フロントエンドアプリは実行時に `public/demo-config.json` を読み込むため、再シード後はブラウザをリロードするだけで反映されます。通常、Vite 開発サーバーの再起動は不要です。

## ヘルスチェックと便利なエンドポイント

| Service | URL | 備考 |
|---------|-----|------|
| API Gateway | http://localhost:8080/api/health | メインのスモークテスト対象 |
| Product Service | http://localhost:8081/health | ホスト実行モード |
| Store Service | http://localhost:8082/health | ホスト実行モード |
| POS Service | http://localhost:8083/health | ホスト実行モード |
| PostgreSQL | localhost:15432/openpos | `openpos` / `openpos_dev` |
| Redis | localhost:16379 | 認証なし |
| RabbitMQ UI | http://localhost:15673 | `openpos` / `openpos_dev` |
| Hydra Public | http://localhost:14444 | OAuth/OIDC パブリックエンドポイント |
| Hydra Admin | http://localhost:14445 | Hydra 管理 API |
| pgAdmin | http://localhost:15080 | `make up-dev` で利用可能 |
| Redis Commander | http://localhost:18081 | `make up-dev` で利用可能 |

動作確認に便利なコマンド:

```bash
docker compose -f infra/compose.yml ps
curl -s http://localhost:8080/api/health
docker compose -f infra/compose.yml logs -f postgres
docker compose -f infra/compose.yml logs -f rabbitmq
```

## トラブルシューティング

### ポートの競合

`open-pos` は共有インフラにデフォルトではないローカルポートを意図的に使用しています:

- PostgreSQL: `15432`
- Redis: `16379`
- RabbitMQ AMQP/UI: `15672` / `15673`
- Hydra Public/Admin: `14444` / `14445`

別のローカルスタックがこれらのポートを使用中の場合は、`make local-demo` や `make docker-demo` を実行する前に停止してください。

### ホスト実行バックエンドが起動しない

`.local/logs/` 配下のサービスごとのログを確認してください。依存関係やビルド成果物を最近変更した場合は、リビルドしてください。

```bash
make local-down
make local-up
```

### コンテナスタックが異常

該当サービスのログを確認し、必要に応じてフルリセットを行ってください。

```bash
docker compose -f infra/compose.yml logs postgres
docker compose -f infra/compose.yml logs rabbitmq
docker compose -f infra/compose.yml down -v
make up
```

### デモ ID や設定が見つからない

`demo-config.json` や `.env.development.local` ファイルが見つからない場合は、再シードしてください。

```bash
make local-seed
make local-smoke
```

フロントエンドに古いデータが表示される場合は、ブラウザタブをリロードしてください。

### モード切替後にスモークチェックが失敗する

一度に一つのバックエンドモードのみが稼働していることを確認してください。

```bash
make local-down
make docker-down-core
```

その後、実際に使用したいモードを起動してください。

### Playwright E2E がローカルで失敗する

ブラウザバンドルを一度インストールしてから、フル検証パスを使用してください。

```bash
pnpm e2e:install
make verify-full
```
