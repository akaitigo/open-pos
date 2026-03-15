package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 値引き理由コードエンティティ。
 * 手動値引き時に選択必須の理由コードを管理する。
 */
@Entity
@Table(name = "discount_reasons", schema = "pos_schema")
class DiscountReasonEntity : BaseEntity() {
    @Column(name = "code", nullable = false, length = 20)
    lateinit var code: String

    @Column(name = "description", nullable = false, length = 255)
    lateinit var description: String

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
