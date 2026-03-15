package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * 日次売上集計エンティティ。
 * 店舗ごとに1日1レコード保持する。
 * 金額はすべて銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "daily_sales", schema = "analytics_schema")
class DailySalesEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: java.util.UUID

    @Column(name = "date", nullable = false)
    lateinit var date: LocalDate

    @Column(name = "gross_amount", nullable = false)
    var grossAmount: Long = 0

    @Column(name = "net_amount", nullable = false)
    var netAmount: Long = 0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long = 0

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = 0

    @Column(name = "transaction_count", nullable = false)
    var transactionCount: Int = 0

    @Column(name = "cash_amount", nullable = false)
    var cashAmount: Long = 0

    @Column(name = "card_amount", nullable = false)
    var cardAmount: Long = 0

    @Column(name = "qr_amount", nullable = false)
    var qrAmount: Long = 0
}
