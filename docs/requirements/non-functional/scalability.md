# スケーラビリティ方針

## 基本方針

**サービス独立デプロイ**: 各マイクロサービスは独立してスケールアウト可能。
ボトルネックになったサービスのみリソースを追加投入する。

## サービス別スケーリング戦略

| サービス | スケーリング方式 | トリガー |
|---------|----------------|---------|
| api-gateway | 水平スケール（Cloud Run） | リクエスト数 |
| pos-service | 水平スケール（Cloud Run） | CPU使用率 |
| product-service | 水平スケール（Cloud Run） | リクエスト数 |
| inventory-service | 水平スケール（Cloud Run） | キュー深度 |
| analytics-service | 水平スケール（Cloud Run） | キュー深度 |
| store-service | 水平スケール（Cloud Run） | CPU使用率 |

- Cloud Run の最小インスタンス数: 1（コールドスタート回避）
- 最大インスタンス数: サービスごとに設定（コスト管理）

## データベーススケーリング

### PostgreSQL スキーマ分離
- テナントは `organization_id` カラムで分離（シングルDB）
- テナント数増加に対してコネクション数は pgBouncer でプール
- 読み取り負荷が高まった場合: Cloud SQL Read Replica 追加

### 将来的なシャーディング
- テナント数が1000を超えた段階でスキーマ分離→DB分離を検討
- `organization_id` をシャードキーとした水平分割

## キャッシュ（Redis / Memorystore）

- cache-aside パターンで DB 読み取り負荷を軽減
- Memorystore のスケールアップで対応（最大300GB）
- キャッシュヒット率 < 80% になったら容量増強を検討

## メッセージキュー（RabbitMQ）

- exchange: `openpos.events`（topic, durable）
- キュー深度が増加した場合: consumer（analytics/inventory）をスケールアウト
- DLQ (`openpos.events.dlq`) で処理失敗メッセージを保全

## テナントごとの利用制限（プラン）

| プラン | 最大店舗数 | 最大端末数/店舗 | API呼び出し/分 |
|--------|----------|--------------|--------------|
| FREE | 1 | 2 | 100 |
| STANDARD | 5 | 10 | 1000 |
| ENTERPRISE | 無制限 | 無制限 | カスタム |

- api-gateway でプランに基づくレート制限を実装（Redis カウンター）

## CDN・静的アセット

- フロントエンド（React/PWA）は Cloud CDN 配信
- 商品画像は Cloud Storage + CDN（署名付きURL）

## 受け入れ条件

- [ ] 1サービスのデプロイが他サービスに影響しない
- [ ] 在庫/分析サービスの consumer 数を増減しても機能が壊れない
- [ ] レート制限超過時に429を返す
