package com.openpos.pos.service

import jakarta.enterprise.context.ApplicationScoped
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * 税額計算エンジン。
 * 標準税率10%、軽減税率8%に対応する外税方式の計算を提供する。
 * 端数は切り捨て（FLOOR）。金額は全て銭単位（10000 = 100円）。
 */
@ApplicationScoped
class TaxCalculationService {
    /**
     * 税抜金額と税率から税額を計算する（端数切り捨て）。
     *
     * @param subtotal 税抜金額（銭単位）
     * @param taxRate 税率（例: "0.10" = 10%, "0.08" = 8%）
     * @return 税額（銭単位）
     */
    fun calculateTax(
        subtotal: Long,
        taxRate: String,
    ): Long {
        if (subtotal <= 0) return 0
        val rate = BigDecimal(taxRate)
        return BigDecimal(subtotal)
            .multiply(rate)
            .setScale(0, RoundingMode.FLOOR)
            .toLong()
    }

    /**
     * 税込合計を計算する。
     *
     * @param subtotal 税抜金額（銭単位）
     * @param taxAmount 税額（銭単位）
     * @return 税込合計（銭単位）
     */
    fun calculateTotal(
        subtotal: Long,
        taxAmount: Long,
    ): Long = subtotal + taxAmount

    /**
     * 明細の小計・税額・合計を一括計算する。
     *
     * @param unitPrice 単価（銭単位）
     * @param quantity 数量
     * @param taxRate 税率文字列（例: "0.10"）
     * @return ItemTaxResult（subtotal, taxAmount, total）
     */
    fun calculateItemTax(
        unitPrice: Long,
        quantity: Int,
        taxRate: String,
    ): ItemTaxResult {
        val subtotal = unitPrice * quantity
        val taxAmount = calculateTax(subtotal, taxRate)
        val total = calculateTotal(subtotal, taxAmount)
        return ItemTaxResult(subtotal = subtotal, taxAmount = taxAmount, total = total)
    }

    /**
     * 税率別集計を生成する（インボイス対応）。
     * 取引明細をグループ化して、税率ごとの課税対象額・税額を集計する。
     *
     * @param items 明細リスト（taxRate, isReduced, subtotal 情報を含む）
     * @return 税率別集計リスト
     */
    fun aggregateTaxSummaries(items: List<TaxableItem>): List<TaxSummaryResult> =
        items
            .groupBy { TaxGroupKey(it.taxRateName, it.taxRate, it.isReducedTax) }
            .map { (key, groupItems) ->
                val taxableAmount = groupItems.sumOf { it.subtotal }
                val taxAmount = calculateTax(taxableAmount, key.taxRate)
                TaxSummaryResult(
                    taxRateName = key.taxRateName,
                    taxRate = key.taxRate,
                    isReduced = key.isReduced,
                    taxableAmount = taxableAmount,
                    taxAmount = taxAmount,
                )
            }
}

/**
 * 明細税額計算結果。
 */
data class ItemTaxResult(
    val subtotal: Long,
    val taxAmount: Long,
    val total: Long,
)

/**
 * 税率別集計の入力データ。
 */
data class TaxableItem(
    val taxRateName: String,
    val taxRate: String,
    val isReducedTax: Boolean,
    val subtotal: Long,
)

/**
 * 税率別集計結果。
 */
data class TaxSummaryResult(
    val taxRateName: String,
    val taxRate: String,
    val isReduced: Boolean,
    val taxableAmount: Long,
    val taxAmount: Long,
)

private data class TaxGroupKey(
    val taxRateName: String,
    val taxRate: String,
    val isReduced: Boolean,
)
