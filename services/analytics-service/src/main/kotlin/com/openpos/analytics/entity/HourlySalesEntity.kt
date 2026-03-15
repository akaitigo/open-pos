package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

/**
 * 時間帯別売上エンティティ。
 * 店舗×日×時間帯（0-23）ごとに1レコード集計する。
 * 金額フィールドはすべて銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "hourly_sales", schema = "analytics_schema")
class HourlySalesEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "sale_date", nullable = false)
    lateinit var saleDate: LocalDate

    @Column(name = "hour", nullable = false)
    var hour: Int = 0

    @Column(name = "total_sales", nullable = false)
    var totalSales: Long = 0

    @Column(name = "transaction_count", nullable = false)
    var transactionCount: Int = 0
}
