# テストテンプレート集

> **Last Updated**: 2026-03-05
> **参照**: [`testing.md`](testing.md) — テスト戦略全体

コピペで使えるテストテンプレート。新機能実装時にこれらをベースに作成する。

---

## 1. Backend 単体テスト（@ParameterizedTest）

金額・税額の境界値テスト例:

```kotlin
package com.openpos.pos.service

import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.InjectMock
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@QuarkusTest
class TaxCalculatorTest {

    @Inject
    lateinit var calculator: TaxCalculator

    @ParameterizedTest
    @CsvSource(
        "10000, STANDARD, 1000",   // ¥100 → 税¥10（標準10%）
        "10000, REDUCED, 800",     // ¥100 → 税¥8（軽減8%）
        "0, STANDARD, 0",          // ¥0
        "1, STANDARD, 0",          // 端数切り捨て
        "999999999, STANDARD, 99999999", // 最大値付近
    )
    fun `税額を正しく計算する`(amount: Long, rate: String, expected: Long) {
        // Arrange
        val taxRate = TaxRate.valueOf(rate)

        // Act
        val result = calculator.calculate(amount, taxRate)

        // Assert
        assertEquals(expected, result)
    }
}
```

---

## 2. Backend 機能テスト（デシジョンテーブル）

税率判定のビジネスルール検証:

```kotlin
package com.openpos.pos.functional

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@QuarkusTest
class TaxRateDecisionTest {

    @Inject
    lateinit var taxRateResolver: TaxRateResolver

    // デシジョンテーブル: 商品区分 × 条件 → 税率
    @ParameterizedTest(name = "category={0}, takeout={1} → rate={2}")
    @CsvSource(
        "FOOD, true, REDUCED",       // 食品 + テイクアウト → 軽減8%
        "FOOD, false, STANDARD",     // 食品 + イートイン → 標準10%
        "ALCOHOL, true, STANDARD",   // 酒類 → 常に標準10%
        "ALCOHOL, false, STANDARD",
        "DAILY_GOODS, true, STANDARD", // 日用品 → 常に標準10%
        "NEWSPAPER, true, REDUCED",  // 定期購読新聞 → 軽減8%
    )
    fun `商品区分とテイクアウト条件から正しい税率を判定する`(
        category: String,
        isTakeout: Boolean,
        expectedRate: String,
    ) {
        val result = taxRateResolver.resolve(
            ProductCategory.valueOf(category),
            isTakeout,
        )
        assertEquals(TaxRate.valueOf(expectedRate), result)
    }
}
```

---

## 3. Backend 結合テスト（Testcontainers）

Repository → 実 PostgreSQL のテスト:

```kotlin
package com.openpos.product.integration

import com.openpos.product.entity.ProductEntity
import com.openpos.product.repository.ProductRepository
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import io.quarkus.test.junit.QuarkusTest
import io.quarkus.test.junit.TestProfile
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
@TestProfile(IntegrationTestProfile::class)
class ProductRepositoryIntegrationTest {

    @Inject lateinit var repository: ProductRepository
    @Inject lateinit var orgIdHolder: OrganizationIdHolder
    @Inject lateinit var tenantFilter: TenantFilterService

    private val testOrgId: UUID = UUID.randomUUID()

    @BeforeEach
    @Transactional
    fun setUp() {
        orgIdHolder.organizationId = testOrgId
        tenantFilter.enableFilter()
        repository.deleteAll()
    }

    @Test
    @Transactional
    fun `商品を保存して取得できる`() {
        // Arrange
        val product = ProductEntity().apply {
            organizationId = testOrgId
            name = "テスト商品"
            price = 10000L  // ¥100
            displayOrder = 1
            isActive = true
        }

        // Act
        repository.persist(product)
        val found = repository.findById(product.id)

        // Assert
        assertNotNull(found)
        requireNotNull(found)
        assertEquals("テスト商品", found.name)
        assertEquals(10000L, found.price)
    }
}
```

---

## 4. Frontend 単体テスト（Vitest + RTL）

```typescript
import { describe, test, expect, vi } from 'vitest'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { ProductCard } from './product-card'

describe('ProductCard', () => {
  const mockProduct = {
    id: '1',
    name: 'テスト商品',
    price: 10000, // ¥100（銭単位）
    isActive: true,
  }

  test('商品名と価格を表示する', () => {
    render(<ProductCard product={mockProduct} onSelect={vi.fn()} />)

    expect(screen.getByText('テスト商品')).toBeInTheDocument()
    expect(screen.getByText('¥100')).toBeInTheDocument()
  })

  test('クリックで onSelect が呼ばれる', async () => {
    const onSelect = vi.fn()
    const user = userEvent.setup()

    render(<ProductCard product={mockProduct} onSelect={onSelect} />)
    await user.click(screen.getByRole('button'))

    expect(onSelect).toHaveBeenCalledWith(mockProduct)
  })
})
```

---

## 5. E2E テスト（Playwright + Page Object Model）

```typescript
import { test, expect } from '@playwright/test'
import { POSPage } from '../pages/pos-page'

test.describe('POS 取引フロー', () => {
  test('商品選択からレシート表示まで完了する', async ({ page }) => {
    const pos = new POSPage(page)
    await pos.goto()

    // 商品を選択してカートに追加
    await pos.searchProduct('テスト商品')
    await pos.addToCart()

    // カート合計を確認
    await expect(pos.cartTotal).toContainText('¥100')

    // 支払い処理
    await pos.proceedToPayment()
    await pos.payCash(100)

    // レシート表示を確認
    await expect(pos.receiptDialog).toBeVisible()
  })
})
```

---

## チェックリスト

新機能実装時のテスト確認:

- [ ] 単体テスト: ロジック・計算の正確性（パラメータ化テスト）
- [ ] 機能テスト: ビジネスルール・仕様の網羅（デシジョンテーブル/状態遷移）
- [ ] 結合テスト: DB・MQ・外部サービスとの統合（Testcontainers）
- [ ] E2E テスト: クリティカルパスの動作確認（Playwright + POM）
- [ ] 金額計算: 0円、1円、端数、最大値、税率切替の境界値
- [ ] カバレッジ: 新規コード 80%+ 確認
