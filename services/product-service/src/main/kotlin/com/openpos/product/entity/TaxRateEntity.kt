package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * 税率エンティティ。
 * インボイス対応: 標準税率(STANDARD)と軽減税率(REDUCED)を管理する。
 * rate は DECIMAL(5,4) で保持（例: 0.1000 = 10%）。
 */
@Entity
@Table(name = "tax_rates", schema = "product_schema")
class TaxRateEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 50)
    lateinit var name: String

    @Column(name = "rate", nullable = false, precision = 5, scale = 4)
    lateinit var rate: BigDecimal

    @Column(name = "tax_type", nullable = false, length = 20)
    var taxType: String = "STANDARD"

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
