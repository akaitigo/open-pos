# ローカル開発手順書

## 日常の開発フロー

```bash
# 1. インフラ起動
make up

# 2. バックエンドサービス起動（開発モード）
./gradlew :services:api-gateway:quarkusDev
./gradlew :services:product-service:quarkusDev

# 3. フロントエンド起動
pnpm dev:pos     # POS端末
pnpm dev:admin   # 管理画面
```

## よくあるトラブル

### ポート競合
dev-stack と同時起動する場合、以下のポートが open-pos 用:
- PostgreSQL: **15432**（dev-stack: 5432）
- Redis: **16379**（dev-stack: 6379）
- RabbitMQ: **15672/15673**

### DB マイグレーションエラー
```bash
# スキーマ再作成
docker compose -f infra/compose.yml down -v
make up
```

### Proto コード生成が失敗する
```bash
buf --version
buf lint  # エラー内容確認
```

### Docker コンテナが起動しない
```bash
docker compose -f infra/compose.yml logs postgres
docker compose -f infra/compose.yml logs rabbitmq

# 全リセット
docker compose -f infra/compose.yml down -v
docker compose -f infra/compose.yml up -d
```

## 接続情報

| サービス | URL | ユーザー | パスワード |
|---------|-----|---------|-----------|
| PostgreSQL | localhost:15432/openpos | openpos | openpos_dev |
| Redis | localhost:16379 | - | - |
| RabbitMQ UI | http://localhost:15673 | openpos | openpos_dev |
| Hydra Public | http://localhost:14444 | - | - |
| Hydra Admin | http://localhost:14445 | - | - |
| pgAdmin | http://localhost:15080 | admin@openpos.dev | admin |
| Redis Commander | http://localhost:18081 | - | - |

## インフラ停止

```bash
make down          # コンテナ停止（データ保持）
docker compose -f infra/compose.yml down -v  # コンテナ + データ削除
```
