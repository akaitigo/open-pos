package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * タイムセールエンティティ。
 * タイムセール/時間帯別価格 (#143)。
 * price は銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "time_sales", schema = "product_schema")
class TimeSaleEntity : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "sale_price", nullable = false)
    var salePrice: Long = 0

    @Column(name = "start_time", nullable = false)
    lateinit var startTime: Instant

    @Column(name = "end_time", nullable = false)
    lateinit var endTime: Instant

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "description")
    var description: String? = null
}
