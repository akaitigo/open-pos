# SaaS 化基盤アーキテクチャ

## 概要

open-pos を SaaS として提供するための基盤設計。マルチテナント・マルチ通貨・プラン別課金・テナント自動プロビジョニングを実現する。

## テナントモデル

```
Organization (テナント)
├── Plan (FREE / STARTER / PROFESSIONAL / ENTERPRISE)
├── Subscription (課金期間・状態)
├── Stores[] (店舗)
│   ├── Terminals[] (端末)
│   └── Staff[] (スタッフ)
├── Products[] (商品)
└── CurrencyRates[] (為替レート)
```

### テナント分離

- **データ分離**: スキーマ共有 + `organization_id` カラム + Hibernate Filter
- **アクセス制御**: gRPC Interceptor で `x-organization-id` をヘッダから抽出
- **リソース制限**: Plan に基づく上限（店舗数・端末数・商品数・スタッフ数）

## プラン体系

| 機能 | FREE | STARTER | PROFESSIONAL | ENTERPRISE |
|------|------|---------|-------------|------------|
| 店舗数 | 1 | 3 | 10 | 無制限 |
| 端末数/店舗 | 1 | 3 | 10 | 無制限 |
| 商品数 | 100 | 1,000 | 10,000 | 無制限 |
| スタッフ数 | 3 | 10 | 50 | 無制限 |
| 通貨 | JPY のみ | 主要5通貨 | 全通貨 | 全通貨 |
| API レート制限 | 100 req/min | 1,000 req/min | 10,000 req/min | カスタム |
| サポート | コミュニティ | メール | 優先メール | 専用担当 |
| SLA | - | 99.5% | 99.9% | 99.99% |
| データ保持期間 | 90日 | 1年 | 3年 | カスタム |

## テナントオンボーディングフロー

```
1. ユーザー登録
   └── ORY Hydra で OIDC アカウント作成

2. Organization 作成
   └── store-service: OrganizationService.create()
       ├── organizations テーブルにレコード作成
       ├── デフォルト Plan (FREE) を割り当て
       └── Subscription レコード作成

3. 初期セットアップ
   └── store-service: ProvisioningService.provision()
       ├── デフォルト店舗作成
       ├── デフォルト端末作成
       ├── オーナースタッフ作成
       └── デフォルト税率作成 (product-service via event)

4. ウェルカムメール送信
   └── notification-service (RabbitMQ event)
```

## マルチ通貨対応

### 為替レートテーブル（V9 migration 済み）

```sql
-- store_schema.currency_rates
CREATE TABLE store_schema.currency_rates (
    id              UUID PRIMARY KEY,
    organization_id UUID NOT NULL,
    base_currency   VARCHAR(3) NOT NULL,  -- 基準通貨 (例: JPY)
    target_currency VARCHAR(3) NOT NULL,  -- 対象通貨 (例: USD)
    rate            DECIMAL(18,8) NOT NULL,
    effective_date  DATE NOT NULL,
    ...
);
```

### 金額処理ルール

1. **内部表現**: 全て銭単位 (BIGINT) で保持。基準通貨は Organization の設定
2. **変換**: `amount * rate` → `FLOOR` で切り捨て
3. **表示**: 通貨コードに基づく `Intl.NumberFormat` でフォーマット
4. **レート更新**: 日次バッチ or 外部 API 連携（ECB, BOJ）

## 課金連携

### Stripe Integration（推奨）

```
Stripe Customer ←→ Organization (1:1)
Stripe Subscription ←→ Subscription (1:1)
Stripe Product ←→ Plan (1:1)
```

### Webhook フロー

```
Stripe → api-gateway → store-service (SubscriptionWebhookHandler)
├── invoice.paid          → Subscription 更新、期間延長
├── invoice.payment_failed → 猶予期間開始、通知送信
├── customer.subscription.deleted → ダウングレード to FREE
└── customer.subscription.updated → Plan 変更反映
```

### 実装ステップ

1. **Phase 1**: Plan 制限の enforcement（リソース上限チェック）
2. **Phase 2**: Stripe Customer/Subscription 連携
3. **Phase 3**: セルフサービスプラン変更 UI
4. **Phase 4**: 使用量ベース課金（API コール数、ストレージ）

## API 設計

### gRPC RPC（store-service に追加）

```protobuf
service PlanService {
  rpc GetPlan(GetPlanRequest) returns (GetPlanResponse);
  rpc ListPlans(ListPlansRequest) returns (ListPlansResponse);
  rpc GetSubscription(GetSubscriptionRequest) returns (GetSubscriptionResponse);
  rpc ChangePlan(ChangePlanRequest) returns (ChangePlanResponse);
}

service ProvisioningService {
  rpc ProvisionTenant(ProvisionTenantRequest) returns (ProvisionTenantResponse);
  rpc DeprovisionTenant(DeprovisionTenantRequest) returns (DeprovisionTenantResponse);
}
```

### リソース制限チェック

各サービスの CREATE 操作前に Plan 制限をチェック:

```kotlin
// PlanEnforcementInterceptor
fun checkLimit(orgId: UUID, resourceType: ResourceType) {
    val plan = planServiceClient.getPlan(orgId)
    val currentCount = getResourceCount(orgId, resourceType)
    if (currentCount >= plan.getLimit(resourceType)) {
        throw PlanLimitExceededException(resourceType, plan.name)
    }
}
```

## セキュリティ考慮事項

- テナント間データ漏洩防止: Hibernate Filter + RLS（将来的に PostgreSQL RLS 併用）
- API キー管理: Organization 単位で API キーを発行・ローテーション
- 課金データ: PCI DSS 準拠（Stripe にカード情報を委譲、自社保持しない）
- GDPR: テナント退会時のデータ完全削除（30日猶予後に物理削除）
