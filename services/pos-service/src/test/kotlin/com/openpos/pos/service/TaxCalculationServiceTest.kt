package com.openpos.pos.service

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@QuarkusTest
class TaxCalculationServiceTest {
    @Inject
    lateinit var taxCalculationService: TaxCalculationService

    // === calculateTax: 境界値テスト ===

    @ParameterizedTest(name = "subtotal={0}, rate={1} → tax={2}")
    @CsvSource(
        "10000, 0.10, 1000", // ¥100 × 10% = ¥10（銭単位）
        "10000, 0.08, 800", // ¥100 × 8% = ¥8
        "0, 0.10, 0", // ¥0 → 税額0
        "-100, 0.10, 0", // 負の金額 → 税額0
        "1, 0.10, 0", // 1銭 × 10% = 0.1銭 → 端数切り捨て = 0
        "10, 0.10, 1", // 10銭 × 10% = 1銭
        "15, 0.10, 1", // 15銭 × 10% = 1.5銭 → 切り捨て = 1
        "999999999, 0.10, 99999999", // 最大値付近
        "10000, 0.00, 0", // 0% 税率
        "30000, 0.08, 2400", // ¥300 × 8% = ¥24
    )
    fun `税額を正しく計算する`(
        subtotal: Long,
        taxRate: String,
        expected: Long,
    ) {
        // Act
        val result = taxCalculationService.calculateTax(subtotal, taxRate)

        // Assert
        assertEquals(expected, result)
    }

    // === calculateTotal ===

    @Test
    fun `税込合計を正しく計算する`() {
        // Arrange
        val subtotal = 10000L
        val taxAmount = 1000L

        // Act
        val total = taxCalculationService.calculateTotal(subtotal, taxAmount)

        // Assert
        assertEquals(11000L, total)
    }

    // === calculateItemTax ===

    @ParameterizedTest(name = "price={0}, qty={1}, rate={2} → sub={3}, tax={4}, total={5}")
    @CsvSource(
        "10000, 1, 0.10, 10000, 1000, 11000", // 単品 標準税率
        "10000, 3, 0.10, 30000, 3000, 33000", // 3個 標準税率
        "10000, 1, 0.08, 10000, 800, 10800", // 単品 軽減税率
        "15000, 2, 0.08, 30000, 2400, 32400", // 2個 軽減税率
        "1, 1, 0.10, 1, 0, 1", // 最小金額
        "0, 5, 0.10, 0, 0, 0", // 0円商品
    )
    fun `明細の税額を一括計算する`(
        unitPrice: Long,
        quantity: Int,
        taxRate: String,
        expectedSubtotal: Long,
        expectedTax: Long,
        expectedTotal: Long,
    ) {
        // Act
        val result = taxCalculationService.calculateItemTax(unitPrice, quantity, taxRate)

        // Assert
        assertEquals(expectedSubtotal, result.subtotal)
        assertEquals(expectedTax, result.taxAmount)
        assertEquals(expectedTotal, result.total)
    }

    // === aggregateTaxSummaries ===

    @Test
    fun `税率別集計を正しく生成する`() {
        // Arrange — 標準税率2品 + 軽減税率1品
        val items =
            listOf(
                TaxableItem("標準税率10%", "0.10", false, 10000),
                TaxableItem("標準税率10%", "0.10", false, 20000),
                TaxableItem("軽減税率8%", "0.08", true, 15000),
            )

        // Act
        val summaries = taxCalculationService.aggregateTaxSummaries(items)

        // Assert
        assertEquals(2, summaries.size)

        val standard = summaries.find { it.taxRateName == "標準税率10%" }
        requireNotNull(standard)
        assertEquals(30000, standard.taxableAmount)
        assertEquals(3000, standard.taxAmount)
        assertEquals(false, standard.isReduced)

        val reduced = summaries.find { it.taxRateName == "軽減税率8%" }
        requireNotNull(reduced)
        assertEquals(15000, reduced.taxableAmount)
        assertEquals(1200, reduced.taxAmount)
        assertEquals(true, reduced.isReduced)
    }

    @Test
    fun `空の明細リストで集計する`() {
        // Act
        val summaries = taxCalculationService.aggregateTaxSummaries(emptyList())

        // Assert
        assertEquals(0, summaries.size)
    }
}
