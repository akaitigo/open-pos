# 日常運用 Runbook

本ドキュメントは open-pos の本番環境における日常運用タスクをまとめたものである。

## 目次

1. [PostgreSQL メンテナンス](#postgresql-メンテナンス)
2. [Redis メモリ監視](#redis-メモリ監視)
3. [RabbitMQ キュー深度チェック](#rabbitmq-キュー深度チェック)
4. [ログローテーション](#ログローテーション)
5. [証明書ローテーション](#証明書ローテーション)
6. [定期チェックスケジュール](#定期チェックスケジュール)

---

## PostgreSQL メンテナンス

### VACUUM

PostgreSQL は MVCC（Multi-Version Concurrency Control）を採用しており、UPDATE/DELETE で不要になったタプルは VACUUM で回収する必要がある。

#### autovacuum の状態確認

```sql
-- autovacuum の設定確認
SHOW autovacuum;
SHOW autovacuum_vacuum_threshold;
SHOW autovacuum_vacuum_scale_factor;

-- 各テーブルの最終 autovacuum 実行日時
SELECT
    schemaname,
    relname,
    last_vacuum,
    last_autovacuum,
    last_analyze,
    last_autoanalyze,
    n_dead_tup,
    n_live_tup
FROM pg_stat_user_tables
ORDER BY n_dead_tup DESC
LIMIT 20;
```

#### 手動 VACUUM（autovacuum が追いつかない場合）

```bash
# 特定テーブルの VACUUM（本番 DB への接続）
psql -h $DB_HOST -p 5432 -U $DB_USER -d openpos -c \
  "VACUUM (VERBOSE, ANALYZE) pos_schema.transactions;"

# 全テーブルの VACUUM（メンテナンスウィンドウ中のみ）
psql -h $DB_HOST -p 5432 -U $DB_USER -d openpos -c \
  "VACUUM (VERBOSE, ANALYZE);"
```

> **注意**: `VACUUM FULL` はテーブルロックを取得するため、本番環境では原則使用しない。必要な場合はメンテナンスウィンドウを設定し、アプリケーションを停止してから実行する。

#### テーブル肥大化チェック

```sql
-- dead tuple の割合が高いテーブルを検出
SELECT
    schemaname || '.' || relname AS table_name,
    n_live_tup,
    n_dead_tup,
    CASE WHEN n_live_tup > 0
        THEN round(100.0 * n_dead_tup / n_live_tup, 2)
        ELSE 0
    END AS dead_pct
FROM pg_stat_user_tables
WHERE n_dead_tup > 1000
ORDER BY dead_pct DESC;
```

### ANALYZE

ANALYZE はテーブルの統計情報を更新し、クエリプランナーの最適化に使用される。

```sql
-- 特定スキーマの全テーブルを ANALYZE
DO $$
DECLARE
    r RECORD;
BEGIN
    FOR r IN SELECT tablename FROM pg_tables WHERE schemaname = 'pos_schema'
    LOOP
        EXECUTE 'ANALYZE pos_schema.' || quote_ident(r.tablename);
    END LOOP;
END $$;
```

### pg_stat_statements

クエリパフォーマンスの監視に `pg_stat_statements` 拡張を使用する。

#### セットアップ確認

```sql
-- 拡張が有効か確認
SELECT * FROM pg_available_extensions WHERE name = 'pg_stat_statements';

-- 有効化されていない場合
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
```

#### スロークエリの特定

```sql
-- 平均実行時間が長いクエリ Top 20
SELECT
    queryid,
    calls,
    round(total_exec_time::numeric, 2) AS total_time_ms,
    round(mean_exec_time::numeric, 2) AS mean_time_ms,
    round(stddev_exec_time::numeric, 2) AS stddev_time_ms,
    rows,
    left(query, 100) AS query_preview
FROM pg_stat_statements
ORDER BY mean_exec_time DESC
LIMIT 20;

-- 総実行時間が長いクエリ Top 20（システム全体への影響が大きい）
SELECT
    queryid,
    calls,
    round(total_exec_time::numeric, 2) AS total_time_ms,
    round(mean_exec_time::numeric, 2) AS mean_time_ms,
    rows,
    left(query, 100) AS query_preview
FROM pg_stat_statements
ORDER BY total_exec_time DESC
LIMIT 20;
```

#### 統計情報のリセット

```sql
-- 定期的にリセットして最新の傾向を確認（週次推奨）
SELECT pg_stat_statements_reset();
```

### コネクションプール監視

```sql
-- 現在のアクティブ接続数
SELECT
    state,
    count(*) AS connection_count
FROM pg_stat_activity
WHERE datname = 'openpos'
GROUP BY state;

-- 長時間実行中のクエリ（5分以上）
SELECT
    pid,
    now() - pg_stat_activity.query_start AS duration,
    state,
    left(query, 100) AS query_preview
FROM pg_stat_activity
WHERE (now() - pg_stat_activity.query_start) > interval '5 minutes'
  AND state != 'idle'
ORDER BY duration DESC;
```

---

## Redis メモリ監視

### メモリ使用量の確認

```bash
# Redis サーバーのメモリ情報
redis-cli -h $REDIS_HOST -p 6379 INFO memory

# 主要メトリクス
redis-cli -h $REDIS_HOST -p 6379 INFO memory | grep -E "used_memory_human|used_memory_peak_human|maxmemory_human|maxmemory_policy|mem_fragmentation_ratio"
```

#### 確認すべきメトリクス

| メトリクス | 説明 | アラート閾値 |
|-----------|------|-------------|
| `used_memory_human` | 現在のメモリ使用量 | maxmemory の 80% |
| `used_memory_peak_human` | ピーク時のメモリ使用量 | 参考値 |
| `maxmemory_human` | 最大メモリ設定 | 設定値確認 |
| `maxmemory_policy` | メモリ上限到達時のポリシー | `allkeys-lru` を推奨 |
| `mem_fragmentation_ratio` | メモリ断片化率 | 1.5 以上で要注意 |

### maxmemory-policy の確認と設定

```bash
# 現在の eviction ポリシー確認
redis-cli -h $REDIS_HOST -p 6379 CONFIG GET maxmemory-policy

# open-pos 推奨: allkeys-lru（キャッシュ用途のため LRU で自動退避）
redis-cli -h $REDIS_HOST -p 6379 CONFIG SET maxmemory-policy allkeys-lru

# maxmemory の設定確認
redis-cli -h $REDIS_HOST -p 6379 CONFIG GET maxmemory
```

### キー数とメモリ分布

```bash
# DB ごとのキー数
redis-cli -h $REDIS_HOST -p 6379 INFO keyspace

# キーパターン別のメモリ使用量を概算
# 注意: KEYS コマンドは本番環境では SCAN を使用すること
redis-cli -h $REDIS_HOST -p 6379 --scan --pattern "openpos:*" | head -20

# 特定キーのメモリ使用量
redis-cli -h $REDIS_HOST -p 6379 MEMORY USAGE "openpos:product:catalog:orgId"
```

### TTL 設定の確認

```bash
# TTL が設定されていないキーの検出（メモリリーク防止）
# SCAN でイテレーションし、TTL が -1 のキーを列挙
redis-cli -h $REDIS_HOST -p 6379 --scan --pattern "openpos:*" | while read key; do
    ttl=$(redis-cli -h $REDIS_HOST -p 6379 TTL "$key")
    if [ "$ttl" = "-1" ]; then
        echo "NO_TTL: $key"
    fi
done
```

> **open-pos のキー規約**: `openpos:{service}:{entity}:{id}` 形式。全キーに TTL を設定すること（デフォルト 1時間）。

---

## RabbitMQ キュー深度チェック

### Management API によるキュー状態確認

```bash
# 全キューの一覧と深度
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS \
  "http://$RABBITMQ_HOST:15672/api/queues" | \
  jq '.[] | {name: .name, messages: .messages, consumers: .consumers, state: .state}'

# 特定 vhost のキュー一覧
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS \
  "http://$RABBITMQ_HOST:15672/api/queues/%2F" | \
  jq '.[] | {name: .name, messages: .messages, messages_ready: .messages_ready, messages_unacknowledged: .messages_unacknowledged}'
```

### DLQ（Dead Letter Queue）残件確認

DLQ にメッセージが滞留している場合、消費側で処理失敗が発生している可能性がある。

```bash
# DLQ キューの一覧と残件数
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS \
  "http://$RABBITMQ_HOST:15672/api/queues" | \
  jq '[.[] | select(.name | contains("dlq") or contains("dead-letter")) | {name: .name, messages: .messages}]'

# DLQ のメッセージ内容を確認（先頭5件）
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS \
  -X POST \
  "http://$RABBITMQ_HOST:15672/api/queues/%2F/{dlq_queue_name}/get" \
  -H "content-type: application/json" \
  -d '{"count": 5, "ackmode": "ack_requeue_true", "encoding": "auto"}'
```

#### DLQ 対応手順

1. DLQ のメッセージ内容を確認し、失敗原因を特定
2. アプリケーションログと照合（`event_id` で検索）
3. 原因を修正後、メッセージを元のキューにリプレイ:

```bash
# DLQ から元のキューへメッセージを移動（RabbitMQ Shovel プラグイン使用）
rabbitmqctl eval 'rabbit_shovel_status:status().'

# または Management UI からメッセージを move
# RabbitMQ Management UI: http://$RABBITMQ_HOST:15672/#/queues
```

### キュー深度アラート閾値

| キュー | 通常時 | 警告閾値 | クリティカル閾値 |
|-------|-------|---------|----------------|
| 通常キュー | 0-100 | 1,000 | 10,000 |
| DLQ | 0 | 1 | 100 |
| イベントキュー | 0-500 | 5,000 | 50,000 |

### コンシューマー数の確認

```bash
# コンシューマーが 0 のキュー（処理が停止している可能性）
curl -s -u $RABBITMQ_USER:$RABBITMQ_PASS \
  "http://$RABBITMQ_HOST:15672/api/queues" | \
  jq '[.[] | select(.consumers == 0 and .messages > 0) | {name: .name, messages: .messages}]'
```

---

## ログローテーション

### アプリケーションログ

open-pos の各マイクロサービスは JSON 形式でログを出力する。Kubernetes 環境では stdout に出力し、ログ収集基盤（Fluentd/Fluent Bit）で集約する。

#### Kubernetes 環境

```bash
# Pod のログ確認（直近100行）
kubectl logs -n openpos deployment/pos-service --tail=100

# ログのフォロー
kubectl logs -n openpos deployment/pos-service -f

# 前回のコンテナのログ（OOM Kill 等で再起動した場合）
kubectl logs -n openpos deployment/pos-service --previous
```

#### Docker Compose 環境（開発/ステージング）

`docker-compose.yml` でログドライバーを設定:

```yaml
services:
  pos-service:
    logging:
      driver: "json-file"
      options:
        max-size: "50m"
        max-file: "5"
```

```bash
# Docker のログファイルサイズ確認
docker system df -v | grep -A 5 "CONTAINER"

# 手動でのログクリア（緊急時のみ）
# truncate -s 0 /var/lib/docker/containers/{container_id}/{container_id}-json.log
```

### PostgreSQL ログ

```bash
# PostgreSQL のログ設定確認
psql -h $DB_HOST -p 5432 -U $DB_USER -d openpos -c "
    SHOW log_directory;
    SHOW log_filename;
    SHOW log_rotation_age;
    SHOW log_rotation_size;
    SHOW log_min_duration_statement;
"
```

推奨設定:

| パラメータ | 推奨値 | 説明 |
|-----------|-------|------|
| `log_rotation_age` | `1d` | 1日ごとにログファイルをローテーション |
| `log_rotation_size` | `100MB` | 100MB でローテーション |
| `log_min_duration_statement` | `1000` | 1秒以上のクエリをログ出力 |
| `log_checkpoints` | `on` | チェックポイント情報をログ出力 |

---

## 証明書ローテーション

### TLS 証明書

#### 有効期限の確認

```bash
# サーバー証明書の有効期限確認
echo | openssl s_client -connect $APP_HOST:443 2>/dev/null | \
  openssl x509 -noout -dates

# 有効期限の日数を計算
expiry_date=$(echo | openssl s_client -connect $APP_HOST:443 2>/dev/null | \
  openssl x509 -noout -enddate | cut -d= -f2)
expiry_epoch=$(date -d "$expiry_date" +%s)
current_epoch=$(date +%s)
days_left=$(( (expiry_epoch - current_epoch) / 86400 ))
echo "TLS 証明書の残り有効日数: $days_left 日"
```

#### 証明書更新手順

Let's Encrypt（cert-manager）を使用している場合:

```bash
# cert-manager の証明書状態確認
kubectl get certificate -n openpos
kubectl describe certificate openpos-tls -n openpos

# 手動更新（通常は自動更新される）
kubectl delete secret openpos-tls -n openpos
# cert-manager が自動的に再発行する
```

手動管理の場合:

```bash
# 1. 新しい証明書を取得（CA から発行）
# 2. Secret を更新
kubectl create secret tls openpos-tls \
  --cert=new-cert.pem \
  --key=new-key.pem \
  -n openpos \
  --dry-run=client -o yaml | kubectl apply -f -

# 3. Ingress/Pod を再起動して新しい証明書を読み込み
kubectl rollout restart deployment -n openpos
```

### OAuth2 クライアントシークレット（ORY Hydra）

```bash
# 現在のクライアント情報確認
hydra clients get $CLIENT_ID \
  --endpoint http://$HYDRA_ADMIN_HOST:4445

# クライアントシークレットのローテーション
hydra clients update $CLIENT_ID \
  --endpoint http://$HYDRA_ADMIN_HOST:4445 \
  --secret $NEW_CLIENT_SECRET

# Kubernetes Secret の更新
kubectl create secret generic hydra-client-secret \
  --from-literal=client-secret=$NEW_CLIENT_SECRET \
  -n openpos \
  --dry-run=client -o yaml | kubectl apply -f -

# アプリケーションの再起動
kubectl rollout restart deployment/api-gateway -n openpos
```

### JWT 署名鍵

ORY Hydra の JWT 署名鍵は自動ローテーションされるが、確認方法は以下の通り:

```bash
# JWKS エンドポイントで公開鍵を確認
curl -s http://$HYDRA_PUBLIC_HOST:4444/.well-known/jwks.json | jq .

# 鍵のローテーション（Hydra Admin API）
hydra keys create hydra.openid.id-token \
  --endpoint http://$HYDRA_ADMIN_HOST:4445 \
  --alg RS256
```

---

## 定期チェックスケジュール

### 日次チェック

| 時刻 | タスク | 担当 | 確認項目 |
|------|-------|------|---------|
| 09:00 | ヘルスチェック | 運用担当 | 全サービスの `/q/health` エンドポイント |
| 09:00 | DLQ 残件確認 | 運用担当 | DLQ にメッセージが滞留していないか |
| 09:00 | Redis メモリ確認 | 運用担当 | `used_memory` が閾値以下か |
| 09:00 | エラーログ確認 | 運用担当 | 直近24時間の ERROR/WARN ログ件数 |

### 週次チェック

| 曜日 | タスク | 担当 | 確認項目 |
|------|-------|------|---------|
| 月 | PostgreSQL スロークエリ | DBA/開発 | `pg_stat_statements` でのスロークエリ確認 |
| 月 | コネクションプール | DBA/開発 | アクティブ接続数の推移 |
| 水 | RabbitMQ キュー深度推移 | 運用担当 | キュー深度の傾向分析 |
| 金 | ディスク使用量 | インフラ | データベース、ログ、Redis の容量 |

### 月次チェック

| タスク | 担当 | 確認項目 |
|-------|------|---------|
| TLS 証明書有効期限 | インフラ | 残り30日以内の証明書がないか |
| OAuth2 シークレット | セキュリティ | シークレットの定期ローテーション |
| PostgreSQL VACUUM 状態 | DBA | dead tuple の蓄積状況 |
| バックアップ復元テスト | インフラ | バックアップから実際に復元できるか確認 |
| 依存ライブラリ脆弱性 | 開発 | Dependabot/Trivy のアラート確認 |

### 四半期チェック

| タスク | 担当 | 確認項目 |
|-------|------|---------|
| DR ドリル | インフラ | `docs/runbook/disaster-recovery.md` に基づく復旧訓練 |
| インシデント振り返り | 全員 | 四半期のインシデント傾向分析 |
| キャパシティプランニング | インフラ/開発 | リソース使用量の推移と予測 |

---

## 関連ドキュメント

- [docs/runbook/incident-response.md](incident-response.md): インシデント対応手順
- [docs/runbook/disaster-recovery.md](disaster-recovery.md): 災害復旧手順
- [docs/runbook/release.md](release.md): リリースチェックリスト
- [docs/deployment-guide.md](../deployment-guide.md): 本番デプロイ手順
