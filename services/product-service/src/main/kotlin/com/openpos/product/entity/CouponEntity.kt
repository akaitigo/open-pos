package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * クーポンエンティティ。
 * 割引と紐付けたコードベースのクーポンを管理する。
 * maxUses で利用回数の上限を制御（null は無制限）。
 */
@Entity
@Table(name = "coupons", schema = "product_schema")
class CouponEntity : BaseEntity() {
    @Column(name = "discount_id", nullable = false)
    lateinit var discountId: UUID

    @Column(name = "code", nullable = false, unique = true, length = 50)
    lateinit var code: String

    @Column(name = "max_uses")
    var maxUses: Int? = null

    @Column(name = "used_count", nullable = false)
    var usedCount: Int = 0

    @Column(name = "valid_from")
    var validFrom: Instant? = null

    @Column(name = "valid_until")
    var validUntil: Instant? = null
}
