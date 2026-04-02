package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

/**
 * 商品別売上集計エンティティ。
 * 店舗・日・商品ごとに1レコード保持する。
 * 金額はすべて銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "product_sales", schema = "analytics_schema")
class ProductSalesEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "date", nullable = false)
    lateinit var date: LocalDate

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "product_name", nullable = false)
    var productName: String = ""

    @Column(name = "category_id")
    var categoryId: UUID? = null

    @Column(name = "category_name", nullable = false)
    var categoryName: String = ""

    @Column(name = "quantity_sold", nullable = false)
    var quantitySold: Int = 0

    @Column(name = "total_amount", nullable = false)
    var totalAmount: Long = 0

    @Column(name = "cost_amount", nullable = false)
    var costAmount: Long = 0

    @Column(name = "transaction_count", nullable = false)
    var transactionCount: Int = 0
}
