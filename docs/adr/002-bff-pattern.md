# ADR-002: BFFパターン（api-gatewayによるREST↔gRPC変換）

## Status

Accepted（2024-01-01）

## Context

フロントエンド（React/PWA）はHTTPS/RESTで通信するが、バックエンドサービス間はgRPCで通信する。
ブラウザからgRPCを直接呼び出すことは技術的制約（HTTP/2フレームの制御）から困難であり、
認証・テナント制御・プロトコル変換を担う中間レイヤーが必要。

## Decision

**api-gateway を BFF（Backend For Frontend）として実装**し、以下の責務を持たせる。

1. JWT検証（ORY Hydra Introspection）
2. organization_id を JWT クレームから抽出し gRPC metadata に注入
3. REST リクエストを gRPC 呼び出しに変換してバックエンドサービスへ転送
4. レート制限・CORS・リクエストログ

```
Browser (REST/HTTPS)
  → api-gateway
    → JWTを検証
    → organization_idを抽出
    → gRPC メタデータに注入
    → バックエンドサービス (gRPC)
```

## Rationale（採用理由）

- **セキュリティ境界の明確化**: バックエンドサービスをインターネット非公開にできる
- **プロトコル変換の集約**: 各サービスに REST エンドポイントを実装する必要がない
- **横断的関心事の一元化**: 認証・ログ・レート制限を1箇所で管理
- **フロントエンド最適化**: フロントエンドに最適化したAPIレスポンス形式を定義できる

## 代替案

| 案 | 棄却理由 |
|----|---------|
| gRPC-Web（直接呼び出し） | Envoy Proxy 等の追加インフラが必要。実装複雑度が増す |
| 各サービスが REST 公開 | 認証ロジックが各サービスに分散。横断的変更が困難 |
| GraphQL Gateway | 学習コストが高い。チームの習熟度が低い |

## Consequences

- **ポジティブ**: バックエンドサービスの変更がフロントエンドに直接影響しない。セキュリティ制御が集中。
- **ネガティブ**: api-gateway が SPOF になるリスク。→ Cloud Run で複数インスタンス・最小1台設定で対応。
- **パフォーマンス**: gRPC 変換のオーバーヘッドは < 5ms と見積もる（許容範囲）。
