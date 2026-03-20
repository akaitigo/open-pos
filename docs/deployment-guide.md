# 本番デプロイガイド

このガイドでは、open-pos を GCP 上の Kubernetes クラスターにデプロイする手順を説明します。

## 前提条件

- GKE クラスター (1.28+) または互換性のある K8s 環境
- 対象クラスターに接続済みの `kubectl`
- Cloud SQL (PostgreSQL 17) インスタンスがプロビジョニング済み
- Memorystore (Redis 7) インスタンスがプロビジョニング済み
- コンテナレジストリへのアクセス (ghcr.io/akaitigo/open-pos または独自レジストリ)
- ORY Hydra v2.2 がデプロイ・設定済み
- RabbitMQ 4 クラスター (CloudAMQP またはセルフマネージド)
- `buf` CLI (Proto コード生成用、ビルド時のみ必要)

## デプロイ順序

依存関係を満たすため、以下の順序でマニフェストを適用してください。

```bash
# 1. Namespace
kubectl apply -f infra/k8s/namespace.yaml

# 2. Secrets (DB 認証情報、Hydra シークレット、RabbitMQ 認証情報)
kubectl apply -f infra/k8s/secrets.yaml

# 3. ConfigMaps (サービスごとの環境変数オーバーライド)
kubectl apply -f infra/k8s/configmaps.yaml

# 4. データベースマイグレーション (Flyway Job — サービス起動前に完了が必要)
kubectl apply -f infra/k8s/flyway-job.yaml
kubectl wait --for=condition=complete --timeout=300s job/flyway-migrate -n openpos

# 5. バックエンドサービス (gRPC サービスを先に、次に api-gateway)
kubectl apply -f infra/k8s/product-service.yaml
kubectl apply -f infra/k8s/store-service.yaml
kubectl apply -f infra/k8s/pos-service.yaml
kubectl apply -f infra/k8s/inventory-service.yaml
kubectl apply -f infra/k8s/analytics-service.yaml
kubectl apply -f infra/k8s/api-gateway.yaml

# 6. Ingress (api-gateway とフロントエンドを外部公開)
kubectl apply -f infra/k8s/ingress.yaml
```

## Flyway によるデータベースマイグレーション

各バックエンドサービスは起動時に Flyway マイグレーションを実行します (`quarkus.flyway.migrate-at-start=true`)。本番デプロイでは、サービス起動前にマイグレーションを実行する専用の Kubernetes Job を使用してください。

```yaml
apiVersion: batch/v1
kind: Job
metadata:
  name: flyway-migrate
  namespace: openpos
spec:
  template:
    spec:
      containers:
        - name: flyway
          image: ghcr.io/akaitigo/open-pos/flyway-migrate:latest
          env:
            - name: DB_URL
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-url
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-user
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: openpos-secrets
                  key: db-password
      restartPolicy: Never
  backoffLimit: 3
```

マイグレーションファイルの配置場所:

```
services/{service}/src/main/resources/db/migration/V{N}__{description}.sql
```

サービスごとのスキーマ:

| Service | Schema |
|---------|--------|
| pos-service | `pos_schema` |
| product-service | `product_schema` |
| inventory-service | `inventory_schema` |
| analytics-service | `analytics_schema` |
| store-service | `store_schema` |

## シークレット管理

本番環境では GCP Secret Manager を使用してください。各サービスの `application.properties` は `${ENV_VAR}` プレースホルダーに対応しています。

| Secret | 使用サービス | 説明 |
|--------|-------------|------|
| `DB_URL` | 全サービス | JDBC 接続文字列 |
| `DB_USER` | 全サービス | データベースユーザー名 |
| `DB_PASSWORD` | 全サービス | データベースパスワード |
| `REDIS_URL` | 全サービス | Redis 接続 URI |
| `RABBITMQ_HOST` | pos, inventory, analytics | RabbitMQ ホスト名 |
| `RABBITMQ_PORT` | pos, inventory, analytics | RabbitMQ AMQP ポート |
| `RABBITMQ_USER` | pos, inventory, analytics | RabbitMQ ユーザー名 |
| `RABBITMQ_PASS` | pos, inventory, analytics | RabbitMQ パスワード |
| `HYDRA_JWKS_URL` | api-gateway | Hydra JWKS エンドポイント |
| `HYDRA_ISSUER` | api-gateway | Hydra トークン発行者 URL |
| `OTEL_ENDPOINT` | 全サービス | OpenTelemetry Collector |

Kubernetes Secret の作成:

```bash
kubectl create secret generic openpos-secrets \
  --namespace=openpos \
  --from-literal=db-url='jdbc:postgresql://CLOUD_SQL_IP:5432/openpos' \
  --from-literal=db-user='openpos' \
  --from-literal=db-password='STRONG_PASSWORD' \
  --from-literal=redis-url='redis://MEMORYSTORE_IP:6379' \
  --from-literal=rabbitmq-host='RABBITMQ_HOST' \
  --from-literal=rabbitmq-pass='RABBITMQ_PASSWORD'
```

## ヘルスチェックの確認

デプロイ後、すべてのサービスが正常であることを確認してください。

```bash
# Pod の状態を確認
kubectl get pods -n openpos

# api-gateway のヘルスチェック (HTTP)
kubectl port-forward svc/api-gateway 8080:80 -n openpos &
curl http://localhost:8080/api/health/live

# gRPC サービスのヘルスチェック (grpcurl 使用)
kubectl port-forward svc/product-service 9001:9001 -n openpos &
grpcurl -plaintext localhost:9001 grpc.health.v1.Health/Check

# すべてのサービスが SERVING または UP と報告されるはずです
```

K8s の readiness probe と liveness probe は各サービスマニフェストで設定されています。

| Service | Health Path | Port |
|---------|-----------|------|
| api-gateway | `/api/health/live` | 8080 |
| product-service | `/health` | HTTP aux port |
| store-service | `/health` | HTTP aux port |
| pos-service | `/health` | HTTP aux port |
| inventory-service | `/health` | HTTP aux port |
| analytics-service | `/health` | HTTP aux port |

## ロールバック手順

### アプリケーションのロールバック

```bash
# Deployment を前のリビジョンにロールバック
kubectl rollout undo deployment/api-gateway -n openpos
kubectl rollout undo deployment/product-service -n openpos
# ... 各サービスについて繰り返す

# ロールバックの確認
kubectl rollout status deployment/api-gateway -n openpos
```

### データベースのロールバック

Flyway は SQL マイグレーションの自動ロールバックをサポートしていません。元に戻すには以下の手順を実施してください。

1. **対象バージョンを特定**: 各スキーマの `flyway_schema_history` テーブルを確認
2. **修正マイグレーションを適用**: 新しい `V{N+1}__revert_{description}.sql` を作成
3. **本番環境で適用済みのマイグレーションファイルは削除・修正しない**

緊急ロールバックの場合:

```bash
# データベースバックアップから復元 (Cloud SQL)
gcloud sql backups restore BACKUP_ID --restore-instance=INSTANCE_NAME

# または手動バックアップから復元
psql -h CLOUD_SQL_IP -U openpos -d openpos < backup.sql
```

### 完全ロールバックチェックリスト

1. すべてのサービスをスケールダウン: `kubectl scale deployment --all --replicas=0 -n openpos`
2. スキーマ変更が適用された場合はデータベースを復元
3. 前バージョンのコンテナイメージタグをデプロイ
4. スケールバックアップ: `kubectl scale deployment --all --replicas=2 -n openpos`
5. ヘルスチェックが通ることを確認
6. チームに通知し、ポストモーテムを作成

## モニタリング

- **Prometheus**: すべてのサービスが `/q/metrics` を公開 (Micrometer)
- **OpenTelemetry**: `OTEL_ENDPOINT` 経由の分散トレーシング
- **構造化ログ**: 本番環境では JSON 形式 (`%prod.quarkus.log.console.json=true`)
- **Grafana ダッシュボード**: `infra/grafana/` に事前設定済み

## フロントエンドのデプロイ

SPA フロントエンドをビルドし、Cloud CDN または静的ホスティングサービスにデプロイします。

```bash
pnpm install
VITE_API_URL=https://api.your-domain.com pnpm build

# Cloud Storage にアップロード
gsutil -m rsync -r apps/pos-terminal/dist gs://your-bucket/pos-terminal
gsutil -m rsync -r apps/admin-dashboard/dist gs://your-bucket/admin-dashboard
```

以下のビルド時環境変数を設定してください。

| 変数 | 説明 |
|------|------|
| `VITE_API_URL` | 本番 api-gateway の URL |
| `VITE_HYDRA_PUBLIC_URL` | 本番 Hydra パブリックエンドポイント |
| `VITE_OIDC_CLIENT_ID` | アプリごとの OAuth2 クライアント ID |
| `VITE_OIDC_REDIRECT_URI` | アプリごとの OAuth2 コールバック URL |
