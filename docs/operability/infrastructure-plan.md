# インフラストラクチャ計画

## GCPリージョン選択

### 推奨構成

| 環境 | リージョン | 用途 |
|------|---------|------|
| **本番（Primary）** | `asia-northeast1`（東京） | メインサービス |
| **DR（Secondary）** | `asia-northeast2`（大阪） | フェイルオーバー |
| **Staging** | `asia-northeast1`（東京） | 本番と同一リージョン |

### 選定理由
- ユーザーの大半が日本国内 → 東京リージョンが最低レイテンシ
- DR は大阪リージョン（地理的に離れた国内リージョン）
- Cloud SQL のクロスリージョンレプリカで DR 対応

### サービス別構成

| サービス | 構成 |
|---------|------|
| **GKE** | Regional cluster（asia-northeast1-a, b, c） |
| **Cloud SQL** | HA構成 + 大阪リードレプリカ |
| **Memorystore (Redis)** | Standard tier with replica |
| **Cloud Storage** | Dual-region (asia1) |

## DRドリル計画

### RTO / RPO 目標

| 障害レベル | RTO | RPO |
|-----------|-----|-----|
| 単一Pod障害 | 30秒（自動復旧） | 0 |
| 単一ノード障害 | 5分 | 0 |
| AZ障害 | 15分 | 0 |
| リージョン障害 | 4時間 | 1時間 |

### ドリル実施手順

#### 1. バックアップ復元テスト（月次）
```bash
# Cloud SQL のバックアップからリストア
gcloud sql backups restore BACKUP_ID --restore-instance=openpos-db-restore-test

# データ整合性確認
psql -h RESTORE_HOST -U openpos -c "SELECT count(*) FROM pos_schema.transactions;"
```

#### 2. フェイルオーバーテスト（四半期）
```bash
# Cloud SQL HA フェイルオーバー
gcloud sql instances failover openpos-db

# Memorystore フェイルオーバー
gcloud redis instances failover openpos-redis --region=asia-northeast1
```

#### 3. リージョン障害シミュレーション（年次）
- Primary リージョンの全サービスを停止
- DR リージョンへの切り替え手順を実行
- E2E テストで動作確認
- 切り戻し手順を実行

### ドリル記録テンプレート

| 項目 | 記録 |
|------|------|
| 実施日 | |
| 実施者 | |
| ドリル種類 | |
| 開始時刻 | |
| 復旧完了時刻 | |
| 実測RTO | |
| データ損失有無 | |
| 発見された問題 | |
| 改善アクション | |
