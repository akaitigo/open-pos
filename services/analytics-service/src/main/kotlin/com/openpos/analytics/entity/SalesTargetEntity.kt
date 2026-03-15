package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.util.UUID

/**
 * 売上目標エンティティ。
 * 組織×店舗×月ごとの売上目標を管理する。
 * 金額は銭単位（BIGINT: 10000 = 100円）。
 */
@Entity
@Table(name = "sales_targets", schema = "analytics_schema")
class SalesTargetEntity : BaseEntity() {
    @Column(name = "store_id")
    var storeId: UUID? = null

    @Column(name = "target_month", nullable = false)
    lateinit var targetMonth: LocalDate

    @Column(name = "target_amount", nullable = false)
    var targetAmount: Long = 0
}
