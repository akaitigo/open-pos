package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant

/**
 * 割引エンティティ。
 * PERCENTAGE（パーセント割引）または FIXED_AMOUNT（定額割引）の割引を管理する。
 * value は BIGINT で保持。PERCENTAGE の場合は「10 = 10%」、FIXED_AMOUNT の場合は銭単位。
 * appliesTo で TRANSACTION（取引全体）または PRODUCT（個別商品）への適用を制御する。
 */
@Entity
@Table(name = "discounts", schema = "product_schema")
class DiscountEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    @Column(name = "discount_type", nullable = false, length = 20)
    lateinit var discountType: String

    @Column(name = "value", nullable = false)
    var value: Long = 0

    @Column(name = "applies_to", nullable = false, length = 20)
    var appliesTo: String = "TRANSACTION"

    @Column(name = "valid_from")
    var validFrom: Instant? = null

    @Column(name = "valid_until")
    var validUntil: Instant? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
