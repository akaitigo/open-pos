# 災害復旧計画 (Disaster Recovery Plan)

## 概要

open-pos システムの災害復旧手順を定義する。RPO（目標復旧時点）およびRTO（目標復旧時間）を最小化し、ビジネス継続性を確保する。

## 復旧目標

| 指標 | 目標値 | 備考 |
|-----|--------|------|
| RPO | 1時間 | 最大1時間分のデータ損失を許容 |
| RTO | 4時間 | 障害発生から4時間以内にサービス復旧 |

## バックアップ戦略

### PostgreSQL

| 種別 | 頻度 | 保持期間 | 方法 |
|------|------|---------|------|
| フルバックアップ | 日次 (03:00 JST) | 30日 | `pg_dump` → GCS |
| WAL アーカイブ | 継続的 | 7日 | Cloud SQL 自動バックアップ |
| ポイントインタイム復旧 | - | 7日間以内 | Cloud SQL PITR |

```bash
# 手動バックアップ
./scripts/db-backup.sh

# リストア
./scripts/db-restore.sh <backup-file>
```

### Redis

- Redis はキャッシュ専用。永続データは PostgreSQL に保持。
- 復旧時は Redis を再起動するだけで良い（cache-aside パターン）。
- データ再構築: アプリケーション起動後にキャッシュが自動再構築される。

### RabbitMQ

- メッセージは一時的。未処理メッセージは DLQ に残る。
- `definitions.json` でキュー・Exchange 定義を再構築可能。

## 復旧手順

### レベル1: 単一サービス障害

1. 該当サービスの Pod / コンテナを再起動
2. ヘルスチェック通過を確認
3. ログで正常動作を確認

```bash
# Cloud Run の場合
gcloud run services update <service-name> --region=asia-northeast1

# Docker Compose の場合
docker compose -f infra/compose.yml restart <service-name>
```

### レベル2: データベース障害

1. Cloud SQL のステータスを確認
2. 自動フェイルオーバーが発動しない場合は手動フェイルオーバー
3. フェイルオーバー後、各サービスの DB 接続を確認
4. Flyway マイグレーション状態を確認

```bash
# Cloud SQL フェイルオーバー
gcloud sql instances failover <instance-name>

# マイグレーション確認
./gradlew :services:pos-service:flywayInfo
```

### レベル3: リージョン障害

1. DNS を DR リージョンに切り替え
2. DR リージョンの Cloud SQL レプリカを昇格
3. Cloud Run サービスをDRリージョンにデプロイ
4. ヘルスチェック + 動作確認

### レベル4: 全損（データ含む）

1. GCS から最新バックアップを取得
2. 新規 Cloud SQL インスタンスを作成
3. バックアップからリストア
4. Flyway マイグレーションを実行
5. 全サービスをデプロイ
6. シードデータが必要な場合は `./scripts/seed.sh` を実行

## オフライン端末の復旧

POS 端末はオフライン対応のため、サーバー障害時も取引を継続できる。

1. 端末はローカル DB (Dexie.js) に取引を保存
2. サーバー復旧後、端末は自動的に `SyncOfflineTransactions` を実行
3. `client_id` による冪等性により重複は排除される

## 連絡先

| 役割 | 連絡先 |
|------|--------|
| インフラ担当 | (要設定) |
| バックエンド担当 | (要設定) |
| フロントエンド担当 | (要設定) |

## 訓練

- 四半期ごとに復旧訓練を実施
- 訓練結果はポストモーテム形式で記録
- RPO/RTO 達成率を計測・改善
