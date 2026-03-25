# Production Deployment Guide

## Overview

OpenPOS の本番デプロイは GitHub Actions CD パイプラインを通じて実行される。
staging 環境への自動デプロイ後、GitHub Environments の承認ゲートを経て本番環境にデプロイされる。

## Architecture

```
Git Tag (v*) → CD Pipeline
  ├── version: セマンティックバージョン生成
  ├── build-and-push: Docker イメージビルド & GHCR push
  ├── deploy-staging: ステージング自動デプロイ
  └── deploy-prod: 本番デプロイ（承認必要）
```

## Prerequisites

### GCP Resources

| リソース | 用途 |
|---------|------|
| GKE クラスタ | Kubernetes 実行環境 |
| Cloud SQL (PostgreSQL 17) | データベース |
| Memorystore (Redis 7) | キャッシュ |
| Cloud Pub/Sub or RabbitMQ on GKE | メッセージキュー |
| GCP Secret Manager | シークレット管理 |

### External Secrets Operator

```bash
# ESO インストール（Helm）
helm repo add external-secrets https://charts.external-secrets.io
helm install external-secrets external-secrets/external-secrets \
  -n external-secrets --create-namespace
```

### GitHub Environments

1. Repository Settings > Environments に `production` 環境を作成
2. Required reviewers に承認者を追加
3. 以下の Secrets を設定:
   - `KUBE_CONFIG_PRODUCTION`: base64 エンコードされた kubeconfig
   - `GCP_PROJECT_ID`: GCP プロジェクト ID（Terraform の `var.project_id` と同じ値）

## Deployment Steps

### 1. シークレット設定

GCP Secret Manager にシークレットを登録:

```bash
# Database credentials
gcloud secrets create openpos-db-user --data-file=- <<< "dbuser"
gcloud secrets create openpos-db-password --data-file=- <<< "secure-password"
gcloud secrets create openpos-db-url --data-file=- <<< "jdbc:postgresql://10.x.x.x:5432/openpos"

# Redis
gcloud secrets create openpos-redis-url --data-file=- <<< "redis://10.x.x.x:6379"

# RabbitMQ
gcloud secrets create openpos-rabbitmq-user --data-file=- <<< "rmquser"
gcloud secrets create openpos-rabbitmq-pass --data-file=- <<< "secure-password"

# Hydra (OIDC)
gcloud secrets create openpos-hydra-jwks-url --data-file=- <<< "https://hydra.example.com/.well-known/jwks.json"
gcloud secrets create openpos-hydra-issuer --data-file=- <<< "https://hydra.example.com/"

# Session JWT
gcloud secrets create openpos-session-jwt-secret --data-file=- <<< "base64-encoded-256bit-key"
```

### 2. ConfigMap 作成

```bash
cp infra/k8s/configmap-prod.yaml.example infra/k8s/configmap-prod.yaml
# 環境に合わせて値を編集
kubectl apply -f infra/k8s/configmap-prod.yaml
```

### 3. ExternalSecret 適用

CD パイプライン経由のデプロイでは、GitHub Secrets の `GCP_PROJECT_ID` が
`infra/k8s/external-secret.yaml` 内の `__GCP_PROJECT_ID__` プレースホルダーに
自動で注入される。

**GitHub Secrets に追加が必要:**
- Repository Settings > Secrets and variables > Actions > Environment secrets (`production`)
- Name: `GCP_PROJECT_ID`, Value: Terraform の `project_id` と同じ値

手動デプロイの場合:

```bash
# Terraform output から取得して置換
GCP_PROJECT_ID=$(terraform -chdir=infra/terraform output -raw project_id)
sed -i "s/__GCP_PROJECT_ID__/${GCP_PROJECT_ID}/" infra/k8s/external-secret.yaml
kubectl apply -f infra/k8s/external-secret.yaml

# 同期確認
kubectl get externalsecret -n openpos
kubectl get secret openpos-secrets -n openpos
```

### 4. デプロイ実行

```bash
# タグを作成して push（CD パイプラインが自動起動）
git tag v1.0.0
git push origin v1.0.0
```

パイプライン実行フロー:
1. バージョン生成
2. 6サービスの Docker イメージビルド & push
3. ステージング環境にデプロイ & rollout 待機
4. **GitHub で承認を要求** (production environment)
5. 承認後、本番環境にデプロイ

### 5. デプロイ後の確認

```bash
# Pod 状態確認
kubectl get pods -n openpos

# ログ確認
kubectl logs -n openpos -l app=api-gateway --tail=50

# ヘルスチェック
kubectl exec -n openpos deployment/api-gateway -- curl -s localhost:8080/q/health
```

## Rollback

```bash
# 直前のバージョンに戻す
kubectl rollout undo deployment/api-gateway -n openpos
kubectl rollout undo deployment/product-service -n openpos
kubectl rollout undo deployment/store-service -n openpos
kubectl rollout undo deployment/pos-service -n openpos
kubectl rollout undo deployment/inventory-service -n openpos
kubectl rollout undo deployment/analytics-service -n openpos

# 特定リビジョンに戻す
kubectl rollout undo deployment/api-gateway -n openpos --to-revision=2
```

## Environment Variables

### Secret（GCP Secret Manager 経由）

| Key | 説明 |
|-----|------|
| `DB_USER` | PostgreSQL ユーザー名 |
| `DB_PASSWORD` | PostgreSQL パスワード |
| `DB_URL` | JDBC 接続 URL |
| `REDIS_URL` | Redis 接続 URL |
| `RABBITMQ_USER` | RabbitMQ ユーザー名 |
| `RABBITMQ_PASS` | RabbitMQ パスワード |
| `HYDRA_JWKS_URL` | ORY Hydra JWKS エンドポイント |
| `HYDRA_ISSUER` | ORY Hydra Issuer URL |
| `OPENPOS_SESSION_JWT_SECRET` | セッション JWT 署名鍵（Base64） |

### ConfigMap（非機密設定）

`infra/k8s/configmap-prod.yaml.example` を参照。

## Monitoring

デプロイ後に以下を確認:
- Prometheus メトリクス: `/q/metrics`
- Liveness probe: `/q/health/live`
- Readiness probe: `/q/health/ready`
- Grafana ダッシュボード（設定済みの場合）

## Troubleshooting

### Pod が起動しない

```bash
kubectl describe pod -n openpos <pod-name>
kubectl logs -n openpos <pod-name> --previous
```

### ExternalSecret が同期しない

```bash
kubectl describe externalsecret openpos-secrets -n openpos
# Workload Identity の権限を確認
```

### Flyway マイグレーション失敗

```bash
# マイグレーションログを確認
kubectl logs -n openpos -l app=<service> | grep -i flyway
```
