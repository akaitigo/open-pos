package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

/**
 * 日次売上エンティティ。
 * 店舗×日ごとに1レコード集計する。
 * 金額フィールドはすべて銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "daily_sales", schema = "analytics_schema")
class DailySalesEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "sale_date", nullable = false)
    lateinit var saleDate: LocalDate

    @Column(name = "total_sales", nullable = false)
    var totalSales: Long = 0

    @Column(name = "net_sales", nullable = false)
    var netSales: Long = 0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long = 0

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = 0

    @Column(name = "transaction_count", nullable = false)
    var transactionCount: Int = 0

    @Column(name = "voided_count", nullable = false)
    var voidedCount: Int = 0

    @Column(name = "returned_count", nullable = false)
    var returnedCount: Int = 0

    @Column(name = "cash_amount", nullable = false)
    var cashAmount: Long = 0

    @Column(name = "card_amount", nullable = false)
    var cardAmount: Long = 0

    @Column(name = "qr_amount", nullable = false)
    var qrAmount: Long = 0
}
