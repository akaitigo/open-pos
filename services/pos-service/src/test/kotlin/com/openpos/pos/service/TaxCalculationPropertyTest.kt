package com.openpos.pos.service

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import net.jqwik.api.constraints.IntRange
import net.jqwik.api.constraints.LongRange

/**
 * TaxCalculationService の Property-Based テスト。
 * 金額計算の代数的不変条件をランダム入力で検証する。
 */
class TaxCalculationPropertyTest {
    private val sut = TaxCalculationService()

    // === Arbitraries ===

    @Provide
    fun validTaxRate(): Arbitrary<String> =
        Arbitraries
            .integers()
            .between(0, 10000)
            .map { (it.toDouble() / 10000).toBigDecimal().toPlainString() }

    // === calculateTax properties ===

    @Property
    fun `税額は常に非負である`(
        @ForAll @LongRange(min = 0, max = 100_000_000) subtotal: Long,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val tax = sut.calculateTax(subtotal, taxRate)
        assert(tax >= 0) { "税額が負: subtotal=$subtotal, rate=$taxRate, tax=$tax" }
    }

    @Property
    fun `税額は小計を超えない（税率100%以下）`(
        @ForAll @LongRange(min = 0, max = 100_000_000) subtotal: Long,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val tax = sut.calculateTax(subtotal, taxRate)
        assert(tax <= subtotal) { "税額が小計超過: subtotal=$subtotal, rate=$taxRate, tax=$tax" }
    }

    @Property
    fun `税額は floor(subtotal * rate) と一致する（subtotal が正の場合）`(
        @ForAll @LongRange(min = 1, max = 100_000_000) subtotal: Long,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val tax = sut.calculateTax(subtotal, taxRate)
        val expected =
            java.math
                .BigDecimal(subtotal)
                .multiply(java.math.BigDecimal(taxRate))
                .setScale(0, java.math.RoundingMode.FLOOR)
                .toLong()
        assert(tax == expected) { "税額不一致: tax=$tax, expected=$expected" }
    }

    @Property
    fun `小計0以下なら税額は0`(
        @ForAll @LongRange(min = -1_000_000, max = 0) subtotal: Long,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val tax = sut.calculateTax(subtotal, taxRate)
        assert(tax == 0L) { "小計≤0なのに税額非0: subtotal=$subtotal, tax=$tax" }
    }

    @Property
    fun `税率0なら税額は0`(
        @ForAll @LongRange(min = 0, max = 100_000_000) subtotal: Long,
    ) {
        val tax = sut.calculateTax(subtotal, "0.0")
        assert(tax == 0L) { "税率0なのに税額非0: subtotal=$subtotal, tax=$tax" }
    }

    // === calculateTotal properties ===

    @Property
    fun `合計は小計と税額の和である`(
        @ForAll @LongRange(min = 0, max = 100_000_000) subtotal: Long,
        @ForAll @LongRange(min = 0, max = 10_000_000) taxAmount: Long,
    ) {
        val total = sut.calculateTotal(subtotal, taxAmount)
        assert(total == subtotal + taxAmount) { "合計 != 小計 + 税額" }
    }

    // === calculateItemTax properties ===

    @Property
    fun `明細小計は単価と数量の積である`(
        @ForAll @LongRange(min = 0, max = 1_000_000) unitPrice: Long,
        @ForAll @IntRange(min = 1, max = 999) quantity: Int,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val result = sut.calculateItemTax(unitPrice, quantity, taxRate)
        assert(result.subtotal == unitPrice * quantity) {
            "小計 != 単価*数量: ${result.subtotal} != ${unitPrice * quantity}"
        }
    }

    @Property
    fun `明細合計は小計と税額の和である`(
        @ForAll @LongRange(min = 0, max = 1_000_000) unitPrice: Long,
        @ForAll @IntRange(min = 1, max = 999) quantity: Int,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val result = sut.calculateItemTax(unitPrice, quantity, taxRate)
        assert(result.total == result.subtotal + result.taxAmount) {
            "合計 != 小計+税額: ${result.total} != ${result.subtotal} + ${result.taxAmount}"
        }
    }

    @Property
    fun `明細合計は常に小計以上（税率非負）`(
        @ForAll @LongRange(min = 0, max = 1_000_000) unitPrice: Long,
        @ForAll @IntRange(min = 1, max = 999) quantity: Int,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val result = sut.calculateItemTax(unitPrice, quantity, taxRate)
        assert(result.total >= result.subtotal) {
            "合計 < 小計: total=${result.total}, subtotal=${result.subtotal}"
        }
    }

    // === aggregateTaxSummaries properties ===

    @Property
    fun `集計の税額合計は個別税額合計と一致する`(
        @ForAll @LongRange(min = 1, max = 1_000_000) subtotal1: Long,
        @ForAll @LongRange(min = 1, max = 1_000_000) subtotal2: Long,
        @ForAll("validTaxRate") taxRate: String,
    ) {
        val items =
            listOf(
                TaxableItem("標準税率", taxRate, false, subtotal1),
                TaxableItem("標準税率", taxRate, false, subtotal2),
            )
        val summaries = sut.aggregateTaxSummaries(items)
        val expectedTax = sut.calculateTax(subtotal1 + subtotal2, taxRate)
        assert(summaries.size == 1) { "同一税率で集約されるべき" }
        assert(summaries[0].taxAmount == expectedTax) {
            "集計税額が不一致: ${summaries[0].taxAmount} != $expectedTax"
        }
    }

    @Property
    fun `異なる税率は別グループに集約される`(
        @ForAll @LongRange(min = 1, max = 1_000_000) subtotal1: Long,
        @ForAll @LongRange(min = 1, max = 1_000_000) subtotal2: Long,
    ) {
        val items =
            listOf(
                TaxableItem("標準税率10%", "0.10", false, subtotal1),
                TaxableItem("軽減税率8%", "0.08", true, subtotal2),
            )
        val summaries = sut.aggregateTaxSummaries(items)
        assert(summaries.size == 2) { "異なる税率は別グループであるべき: size=${summaries.size}" }
    }
}
