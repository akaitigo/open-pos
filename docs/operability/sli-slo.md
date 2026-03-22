# SLI / SLO 定義

## サービスレベル目標（SLO）

| サービス | SLI | SLO | 測定方法 |
|---------|-----|-----|---------|
| **API Gateway** | 可用性（2xx+3xx / total） | 99.9%/月 | Prometheus `http_server_requests_seconds_count` |
| **API Gateway** | レイテンシ P95 | < 200ms | Prometheus `http_server_requests_seconds` histogram |
| **API Gateway** | レイテンシ P99 | < 500ms | 同上 |
| **POS Service** | gRPC 成功率 | 99.9%/月 | Prometheus `grpc_server_handled_total` |
| **POS Service** | CreateTransaction P95 | < 100ms | Prometheus gRPC latency histogram |
| **全サービス** | gRPC エラー率 | < 1% | `grpc_server_handled_total{grpc_code!="OK"}` / total |
| **E2E** | POS 会計フロー完了率 | 99.5% | Synthetic monitoring |

## エラーバジェット

| SLO | 月間許容ダウンタイム | 計算 |
|-----|-------------------|------|
| 99.9% | 43分12秒 | 30日 × 24h × 60m × 0.1% |
| 99.5% | 3時間36分 | 30日 × 24h × 60m × 0.5% |

## Prometheus アラートルール

```yaml
# SLO 違反アラート（追加推奨）
- alert: HighErrorRate
  expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.01
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "API error rate > 1% (SLO violation)"

- alert: HighLatency
  expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 0.2
  for: 5m
  labels:
    severity: warning
  annotations:
    summary: "API P95 latency > 200ms (SLO violation)"

- alert: GrpcHighErrorRate
  expr: rate(grpc_server_handled_total{grpc_code!="OK"}[5m]) / rate(grpc_server_handled_total[5m]) > 0.01
  for: 5m
  labels:
    severity: critical
  annotations:
    summary: "gRPC error rate > 1%"
```

## レビューサイクル

- **週次**: SLI ダッシュボード確認
- **月次**: エラーバジェット消費率レビュー
- **四半期**: SLO 目標値の見直し
