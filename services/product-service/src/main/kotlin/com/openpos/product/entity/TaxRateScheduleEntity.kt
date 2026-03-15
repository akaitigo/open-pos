package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * 税率変更スケジュールエンティティ。
 * 指定日に自動で税率を変更するためのスケジュール。
 * applied = true の場合は適用済み。
 */
@Entity
@Table(name = "tax_rate_schedules", schema = "product_schema")
class TaxRateScheduleEntity : BaseEntity() {
    @Column(name = "tax_rate_id", nullable = false)
    lateinit var taxRateId: UUID

    @Column(name = "new_rate", nullable = false, precision = 5, scale = 4)
    lateinit var newRate: BigDecimal

    @Column(name = "effective_date", nullable = false)
    lateinit var effectiveDate: LocalDate

    @Column(name = "applied", nullable = false)
    var applied: Boolean = false
}
