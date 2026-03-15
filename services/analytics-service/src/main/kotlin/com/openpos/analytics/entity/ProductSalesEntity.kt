package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

/**
 * 商品別売上エンティティ。
 * 店舗×商品×日ごとに1レコード集計する。
 * 金額フィールドはすべて銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "product_sales", schema = "analytics_schema")
class ProductSalesEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "product_name", nullable = false, length = 255)
    var productName: String = ""

    @Column(name = "sale_date", nullable = false)
    lateinit var saleDate: LocalDate

    @Column(name = "quantity_sold", nullable = false)
    var quantitySold: Int = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @Column(name = "average_price", nullable = false)
    var averagePrice: Long = 0

    @Column(name = "transaction_count", nullable = false)
    var transactionCount: Int = 0
}
