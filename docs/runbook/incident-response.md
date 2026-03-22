# インシデント対応手順（SOP）

## オンコール体制

| 役割 | 担当 | 連絡先 |
|------|------|--------|
| Primary on-call | (要設定) | (要設定) |
| Secondary on-call | (要設定) | (要設定) |
| エスカレーション先 | (要設定) | (要設定) |

> **TODO**: 本番デプロイ前に上記を埋めること。

## 重大度レベル

| レベル | 定義 | 対応時間 | 例 |
|--------|------|---------|-----|
| **SEV1** | 全サービス停止 | 15分以内に対応開始 | DB障害、api-gatewayダウン |
| **SEV2** | 主要機能停止 | 30分以内に対応開始 | POS決済不可、認証障害 |
| **SEV3** | 一部機能劣化 | 4時間以内に対応開始 | analytics遅延、キャッシュ不整合 |
| **SEV4** | 軽微な問題 | 次営業日に対応 | UIバグ、ログ欠損 |

## 対応フロー

### 1. 検知・通知
- Grafana アラート → Slack 通知（要設定）
- ユーザー報告 → サポート窓口

### 2. 初動対応（15分以内）
1. アラート内容を確認
2. 影響範囲を特定（テナント単体 or 全体）
3. Slack の `#incident` チャンネルでインシデント宣言
4. 重大度レベルを決定

### 3. 調査
1. **Grafana ダッシュボード** でメトリクス確認
   - `http://grafana:3000/d/openpos-overview`
2. **ログ確認**
   ```bash
   kubectl logs -n openpos deployment/{service-name} --tail=100
   ```
3. **gRPC トレース** で Correlation ID (`x-request-id`) を追跡

### 4. 対応
- **スケールアウト**: `kubectl scale deployment/{service} -n openpos --replicas=3`
- **ロールバック**: `kubectl rollout undo deployment/{service} -n openpos`
- **サービス再起動**: `kubectl rollout restart deployment/{service} -n openpos`

### 5. 復旧確認
1. ヘルスチェック: `kubectl get pods -n openpos`
2. E2E スモークテスト実行
3. Grafana でエラー率が正常に戻ったことを確認

### 6. ポストモーテム
- インシデント発生後 3営業日以内に作成
- テンプレート: `docs/templates/postmortem.md`
- ブレームレス、根本原因分析、再発防止策

## よくある障害と対応

| 障害 | 確認方法 | 対応 |
|------|---------|------|
| DB接続断 | `kubectl logs` で `Connection refused` | PostgreSQL Pod確認、pgBouncer再起動 |
| Redis接続断 | メトリクスでcache miss急増 | Redis Pod確認、再起動 |
| RabbitMQ溢れ | Management UI でキュー深度確認 | コンシューマースケールアウト |
| OOM Kill | `kubectl describe pod` で `OOMKilled` | リソースlimits引き上げ |
| E2Eテスト的な503 | api-gateway ログ | Readiness probe確認、依存サービス確認 |
