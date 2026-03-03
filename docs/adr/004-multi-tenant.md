# ADR-004: テナントIDカラム方式（Hibernate Filter + gRPC metadata）

## Status

Accepted（2024-01-01）

## Context

open-pos は複数のテナント（組織）が同一インフラ・同一DBを共有するマルチテナントSaaSである。
テナント間のデータ分離は最重要のセキュリティ要件であり、実装漏れが即座にデータ漏洩につながる。

主要なマルチテナント実装方式:
1. **DB分離**: テナントごとに別DB
2. **スキーマ分離**: テナントごとに別スキーマ
3. **テナントIDカラム方式**: 全テーブルに `organization_id` カラム

## Decision

**テナントIDカラム方式**（全テーブルに `organization_id`）を採用し、
**Hibernate Filter** でアプリケーション層での自動フィルタリングを実装する。

### 実装方法

```kotlin
// エンティティ
@Entity
@FilterDef(name = "tenantFilter",
           parameters = [ParamDef(name = "orgId", type = UUID::class)])
@Filter(name = "tenantFilter", condition = "organization_id = :orgId")
class Product {
    @Column(nullable = false)
    lateinit var organizationId: UUID
}

// インターセプター（全リクエストで適用）
@ApplicationScoped
class TenantFilterInterceptor {
    fun applyFilter(session: Session, organizationId: UUID) {
        session.enableFilter("tenantFilter")
               .setParameter("orgId", organizationId)
    }
}
```

### テナントID伝播

```
JWT (organization_id クレーム)
  → api-gateway 抽出
  → gRPC metadata (x-organization-id)
  → バックエンドサービスのインターセプター
  → Hibernate Filter 適用
```

## Rationale（採用理由）

- **テナント数に上限なし**: DB・スキーマ分離は多数テナントで運用が困難
- **スキーマ管理が単純**: マイグレーションは1回で全テナントに適用
- **Hibernate Filter**: アプリ層で漏れなくフィルタを適用（クエリ書き忘れを防止）

## 代替案

| 案 | 棄却理由 |
|----|---------|
| DB分離（テナント別DB） | テナント数×DBインスタンスのコスト・管理コストが現実的でない |
| スキーマ分離（テナント別スキーマ） | マイグレーション管理が複雑。テナント数増加で接続プール管理が困難 |
| Row Level Security（PostgreSQL RLS） | Kotlin/Hibernate との統合が複雑。デバッグが困難 |

## Consequences

- **ポジティブ**: 単一マイグレーションで全テナントに適用。低インフラコスト。
- **ネガティブ**: Hibernate Filter 未適用のクエリが存在すると全テナントデータが見える。
- **軽減策**:
  - CI でカスタムアーキテクチャテスト（ArchUnit）を実行し、`@Filter` アノテーションなしエンティティを検出
  - インテグレーションテストで別テナントデータが返らないことを全エンドポイントで確認
  - コードレビューチェックリストに「Hibernate Filter 適用確認」を追加
