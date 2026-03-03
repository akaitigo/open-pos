# 開発環境セットアップ

## 前提条件

| ツール | バージョン | インストール方法 |
|--------|-----------|----------------|
| Java (GraalVM CE) | 21 | `mise install java@graalce-21` |
| Kotlin | 2.1+ | `sdk install kotlin` |
| Gradle | 8.12+ | `mise install gradle` |
| Node.js | 22+ | `mise install node@22` |
| pnpm | 9+ | `npm install -g pnpm` |
| Docker | 24+ | [公式サイト](https://docs.docker.com/engine/install/) |
| Docker Compose | v2+ | Docker に同梱 |
| buf | 1.x | `mise install buf` or [公式サイト](https://buf.build/docs/installation) |
| gh | 最新 | `apt install gh` |

## セットアップ手順

### 1. リポジトリクローン
```bash
gh repo clone akaitigo/open-pos
cd open-pos
```

### 2. インフラ起動
```bash
make up
```

PostgreSQL, Redis, RabbitMQ, ORY Hydra が起動します。

### 3. DB 初期化確認
初回起動時に `infra/init-scripts/postgres/01_init.sql` が自動実行され、各スキーマが作成されます。

### 4. Proto コード生成
```bash
make proto
```

### 5. バックエンドビルド
```bash
./gradlew build
```

### 6. フロントエンドセットアップ
```bash
pnpm install
pnpm dev:pos     # POS端末 (http://localhost:5173)
pnpm dev:admin   # 管理画面 (http://localhost:5174)
```

## 動作確認

```bash
# インフラヘルスチェック
docker compose -f infra/compose.yml ps

# PostgreSQL 接続
pgcli -h localhost -p 15432 -U openpos -d openpos

# Redis 接続
redis-cli -p 16379 ping

# RabbitMQ 管理画面
open http://localhost:15673  # admin/openpos_dev
```

## 開発ツール付き起動

```bash
make up-dev
```

追加で pgAdmin (http://localhost:15080) と Redis Commander (http://localhost:18081) が起動します。
