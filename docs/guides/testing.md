# テスト戦略

> **Last Updated**: 2026-03-05
> **リサーチベース**: `docs/research/testing-*.md`（ISTQB/JSTQB 定義準拠）

## 基本方針

**全ての機能実装には、単体テスト・機能テスト・結合テスト・E2Eテストを必ずコードとして含める。**

目的:
- デグレッション（退行）の防止
- 成果物の品質担保
- 安全なリファクタリングの実現

テストピラミッドに従い、下位レベルほど厚く（数多く）、上位レベルほど薄く（少数精鋭）する。

```
        /  E2E  \          ← 少数: クリティカルパスのみ
       /  結合テスト \       ← 中量: 統合点ごと
      / 機能テスト    \      ← 中量: 仕様ベース設計技法
     / 単体テスト      \     ← 大量: Fast/Isolated/Repeatable
```

---

## テストレベル定義

### 1. 単体テスト（Unit Test）

**目的**: 最小単位のロジック検証。欠陥の早期発見と設計へのフィードバック。

| 項目 | Backend | Frontend |
|------|---------|----------|
| フレームワーク | JUnit 5 + `@InjectMock` | Vitest + React Testing Library |
| 対象 | Service / Entity / ユーティリティ | hooks / stores / utils / コンポーネント |
| 場所 | `src/test/kotlin/` | `src/**/*.test.ts(x)` |
| 実行 | `./gradlew :services:{name}:test` | `pnpm --filter {app} test` |
| 速度目標 | 1テスト < 100ms | 1テスト < 50ms |

**ルール**:
- AAA パターン（Arrange-Act-Assert）で記述
- 外部依存（DB/Redis/RabbitMQ/Network）は `@InjectMock` または `vi.mock` で隔離
- 1テスト = 1検証意図（失敗原因を局所化）
- パラメータ化テスト推奨（`@ParameterizedTest` / `test.each`）
- 金額計算・税額計算は境界値テスト必須（0円、1円、端数、最大値、軽減税率切替）

**Backend サンプル**:
```kotlin
@QuarkusTest
class TaxCalculatorTest {

    @Inject
    lateinit var calculator: TaxCalculator

    @ParameterizedTest
    @CsvSource(
        "10000, STANDARD, 1000",   // 100円 → 税10円（銭単位）
        "10000, REDUCED, 800",     // 100円 → 税8円（軽減税率）
        "0, STANDARD, 0",          // 0円
        "1, STANDARD, 0"           // 端数切り捨て
    )
    fun `税額を正しく計算する`(amount: Long, rate: TaxRate, expected: Long) {
        val result = calculator.calculate(amount, rate)
        assertEquals(expected, result)
    }
}
```

**Frontend サンプル**:
```typescript
import { describe, test, expect } from 'vitest'
import { formatMoney } from '@shared-types/openpos'

describe('formatMoney', () => {
  test.each([
    [10000, '¥100'],
    [0, '¥0'],
    [1, '¥0'],     // 端数
    [999999999, '¥9,999,999'],
  ])('formatMoney(%i) = %s', (sen, expected) => {
    expect(formatMoney(sen)).toBe(expected)
  })
})
```

---

### 2. 機能テスト（Functional Test）

**目的**: 機能要件（仕様）を満たすかの検証。テストベース（要求・ユーザーストーリー・受け入れ基準）からの体系的設計。

| 項目 | Backend | Frontend |
|------|---------|----------|
| フレームワーク | JUnit 5 + `@QuarkusTest` | Vitest + RTL |
| 対象 | gRPC サービスの RPC 単位 | ページ/フィーチャー単位のユーザーフロー |
| 場所 | `src/test/kotlin/**/functional/` | `src/**/*.functional.test.ts(x)` |

**設計技法の使い分け**:

| 仕様の性質 | 適用技法 | open-pos での適用例 |
|-----------|---------|-------------------|
| 入力バリデーション | 同値分割 + 境界値分析 | 商品価格、数量、割引率 |
| ビジネスルール | デシジョンテーブル | 税率判定（標準/軽減/免税 × 商品区分） |
| 状態遷移 | 状態遷移テスト | 取引ステータス（DRAFT→FINALIZED→VOIDED） |
| 業務フロー | ユースケーステスト | 取引作成→明細追加→確定→レシート |

**ルール**:
- テストケースにトレーサビリティを持たせる（Issue 番号 or 受け入れ基準 ID）
- 期待結果は観測可能な形で記述（レスポンス値、DB レコード、イベント発行）
- Red → Green（実装前にテストが失敗することを確認）

---

### 3. 結合テスト（Integration Test）

**目的**: コンポーネント間のインターフェース・相互作用が設計通りに成立するかの検証。

| 項目 | 内容 |
|------|------|
| フレームワーク | `@QuarkusTest` + Testcontainers（PostgreSQL, Redis, RabbitMQ） |
| 対象 | Repository ↔ DB、Service ↔ RabbitMQ、gRPC Client ↔ Server、API Gateway REST ↔ gRPC |
| 場所 | `src/test/kotlin/**/integration/` |
| 実行 | `./gradlew :services:{name}:test -Dquarkus.test.profile=integration` |

**統合点カタログ**（テスト対象の明示）:

| 統合点 | Provider | Consumer | テスト方針 |
|--------|----------|----------|-----------|
| DB (PostgreSQL) | PostgreSQL | Repository | Testcontainers で実 DB |
| Cache (Redis) | Redis | CacheService | Testcontainers で実 Redis |
| MQ (RabbitMQ) | RabbitMQ | EventPublisher/Consumer | Testcontainers で実 MQ |
| gRPC サービス間 | pos-service | inventory-service | `@QuarkusTest` で実サービス起動 |
| REST → gRPC | api-gateway | 各 gRPC service | `@QuarkusTest` + gRPC stub |
| Hydra (OAuth2) | ORY Hydra | api-gateway | WireMock でスタブ |

**ルール**:
- 外部依存は Testcontainers で再現（Docker 必須）
- 外部 API（Hydra 等）は WireMock でスタブ化
- テストデータは各テストで生成・後処理（テスト間の干渉排除）
- 失敗時の原因特定のため相関 ID をログ出力
- ビッグバン統合禁止 → インクリメンタル（統合点ごと）

**サンプル**:
```kotlin
@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class ProductRepositoryIntegrationTest {

    @Inject
    lateinit var repository: ProductRepository

    @Test
    fun `商品を保存して取得できる`() {
        // Arrange
        val product = ProductEntity(
            name = "テスト商品",
            price = 10000L,  // ¥100
            organizationId = testOrgId
        )

        // Act
        repository.persist(product)
        val found = repository.findById(product.id)

        // Assert
        assertNotNull(found)
        assertEquals("テスト商品", found?.name)
        assertEquals(10000L, found?.price)
    }
}
```

---

### 4. E2Eテスト（End-to-End Test）

**目的**: ユーザーフロー全体が端から端まで動作することの検証。

| 項目 | 内容 |
|------|------|
| フレームワーク | Playwright（TypeScript） |
| 対象 | クリティカルなユーザーフローのみ（少数精鋭） |
| 場所 | `e2e/` |
| 実行 | `pnpm --filter e2e test` |
| CI | main ブランチ / リリース前に実行 |

**対象フロー（クリティカルパス）**:
1. POS 端末: 商品選択 → カート追加 → 支払 → レシート表示
2. POS 端末: 取引無効化（返品）
3. 管理画面: 商品 CRUD
4. 認証: PIN ログイン → 操作 → ログアウト
5. オフライン: オフライン取引 → オンライン復帰 → 同期

**ルール**:
- E2E は「クリティカルパス」に絞る（広い網羅は下位テストで担う）
- Page Object Model（POM）で UI 要素を抽象化（変更耐性）
- `data-testid` 属性でセレクタを安定化
- 手動 sleep 禁止 → Playwright の自動待機（actionability checks）に寄せる
- 失敗時はスクリーンショット + トレースを CI アーティファクトに保存

**サンプル**:
```typescript
import { test, expect } from '@playwright/test'
import { POSPage } from './pages/pos-page'

test('商品選択からレシート表示まで完了する', async ({ page }) => {
  const pos = new POSPage(page)
  await pos.goto()
  await pos.selectProduct('テスト商品')
  await pos.updateQuantity(2)
  await expect(pos.cartTotal).toContainText('¥200')
  await pos.proceedToPayment()
  await pos.payCash(200)
  await expect(pos.receiptDialog).toBeVisible()
})
```

---

## テスト実行コマンド

```bash
# 日常の検証導線
make test            # Backend 全サービス
make test-apps       # Frontend unit/functional テスト
pnpm test            # packages + apps の unit/functional テスト

# サービス別
./gradlew :services:pos-service:test
./gradlew :services:inventory-service:test

# アプリ別
pnpm --filter pos-terminal test
pnpm --filter admin-dashboard test

# E2E
pnpm e2e:install
pnpm --filter e2e test
make test-e2e

# カバレッジ付き
./gradlew :services:pos-service:test jacocoTestReport
pnpm --filter pos-terminal test -- --coverage
```

## E2E の前提

- `pnpm test` と `make test-apps` は E2E を含めない。日常の高速な回帰確認を優先する。
- E2E は `pnpm test:e2e` または `make test-e2e` で明示実行する。
- 初回のみ `pnpm e2e:install` で Playwright 用 Chromium をインストールする。
- `pnpm test:e2e` は `pos-terminal` と `admin-dashboard` の dev server を自動起動する。

---

## CI パイプライン統合

```
PR 作成/更新時:
  ├── 単体テスト（全サービス・全アプリ）     ← 必須ゲート
  ├── 機能テスト（全サービス・全アプリ）     ← 必須ゲート
  └── 結合テスト（変更サービスのみ）         ← 必須ゲート

main マージ時:
  ├── 結合テスト（全サービス）
  └── E2E テスト（クリティカルパス）

リリース前:
  └── E2E テスト（全シナリオ）
```

**品質ゲート基準**:
- 単体/機能/結合テスト: 全 pass 必須（1件でも失敗したらマージ不可）
- E2E: クリティカルパス全 pass 必須
- カバレッジ: 新規コードの行カバレッジ 80% 以上（既存コードは段階的に向上）
- フレーク率: 再実行で通るテストは原因調査・修正の対象

---

## カバレッジ指標

| 指標 | ツール | 目標 |
|------|--------|------|
| 行カバレッジ（Backend） | JaCoCo | 新規 80%+ |
| 分岐カバレッジ（Backend） | JaCoCo | 新規 70%+ |
| 行カバレッジ（Frontend） | Vitest (v8) | 新規 80%+ |
| 要件カバレッジ | RTM（Issue ↔ テスト紐付け） | 全 P0/P1 Issue |
| 統合点カバレッジ | 統合点カタログ | 全統合点に 1+ テスト |

---

## テストの原則

1. **Red → Green**: 実装前にテストが失敗することを確認（TDD/BDD）
2. **Fast/Isolated/Repeatable**: 単体テストは高速・隔離・反復可能
3. **1テスト = 1検証意図**: 失敗原因の局所化
4. **テストデータの独立性**: テスト間で状態を共有しない
5. **金額計算は境界値テスト必須**: 0円、1円、端数、最大値、税率切替
6. **モックは最小限**: 実装詳細への密着を避ける（状態検証 > ふるまい検証）
7. **E2E は少数精鋭**: クリティカルパスに絞り、広い網羅は下位テストで

## リサーチ参照

テスト戦略の詳細なリサーチ・根拠は以下を参照:
- [単体テスト](../research/testing-unit.md)
- [機能テスト](../research/testing-functional.md)
- [結合テスト](../research/testing-integration.md)
- [E2Eテスト](../research/testing-e2e.md)
