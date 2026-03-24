# マルチリージョン DR 構成ガイド

## アーキテクチャ概要

```
                    ┌─ asia-northeast1 (Tokyo) ─┐     ┌─ asia-northeast2 (Osaka) ─┐
                    │                            │     │                            │
  Cloud LB ───────> │  GKE Autopilot (Primary)   │     │  GKE Autopilot (Standby)   │
  (Global)          │  - 6 microservices         │     │  - 6 microservices (scaled  │
                    │  - 3 nodes                 │     │    down, 1 node)            │
                    │                            │     │                            │
                    │  Cloud SQL PostgreSQL 17   │────>│  Cloud SQL Read Replica    │
                    │  (Regional HA)             │     │  (failover_target=true)     │
                    │                            │     │                            │
                    │  Memorystore Redis 7.2     │     │  Memorystore Redis 7.2     │
                    │  (STANDARD_HA, 4GB)        │     │  (STANDARD_HA, 2GB)        │
                    └────────────────────────────┘     └────────────────────────────┘
```

## リージョン構成

| コンポーネント | Primary (Tokyo) | Secondary (Osaka) |
|-------------|----------------|------------------|
| GKE | Autopilot, 3 nodes | Autopilot, 1 node (standby) |
| Cloud SQL | Regional HA, db-custom-2-8192 | Cross-region read replica, failover target |
| Memorystore | STANDARD_HA, 4GB | STANDARD_HA, 2GB |
| Cloud Storage | dual-region (asia1) | 自動レプリケーション |

## RPO / RTO 目標

| メトリクス | 目標 | 達成方法 |
|----------|------|---------|
| RPO | < 1 min | Cloud SQL PITR + WAL streaming |
| RTO | < 15 min | Read replica promotion + GKE standby |

## フェイルオーバー手順

### 自動フェイルオーバー（リージョン内）

Cloud SQL Regional HA と Memorystore STANDARD_HA が自動処理。アプリケーション側はリトライで対応。

### 手動フェイルオーバー（クロスリージョン）

1. **判断**: Primary リージョン全体の障害を確認
2. **Cloud SQL**: Read replica をプロモーション
   ```bash
   gcloud sql instances promote-replica openpos-production-replica
   ```
3. **GKE**: Secondary クラスタをスケールアップ
   ```bash
   # ConfigMap の DB 接続先を replica に変更
   kubectl --context=gke_PROJECT_asia-northeast2_openpos-production-secondary \
     apply -f infra/k8s/configmap-dr.yaml
   # デプロイメントをスケールアップ
   kubectl --context=gke_PROJECT_asia-northeast2_openpos-production-secondary \
     scale deployment --all --replicas=2
   ```
4. **DNS**: Cloud Load Balancer のバックエンドを切り替え
5. **検証**: ヘルスチェックとスモークテスト実行

### フォールバック（Primary 復帰）

1. 新 Primary（旧 Osaka）からデータをエクスポート
2. Tokyo に新 Cloud SQL インスタンスを作成
3. データをインポート
4. Osaka を read replica に再設定
5. GKE トラフィックを Tokyo に戻す

## Terraform 適用

```bash
cd infra/terraform

# 初期化
terraform init

# プラン確認
terraform plan -var-file=terraform.tfvars

# 適用
terraform apply -var-file=terraform.tfvars
```

## 月次 DR テスト

1. Secondary クラスタへのトラフィック切り替えテスト
2. Cloud SQL replica promotion テスト（テスト環境）
3. バックアップからのリストアテスト
4. RTO 計測と記録
