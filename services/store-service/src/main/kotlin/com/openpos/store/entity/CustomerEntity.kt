package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 顧客エンティティ。
 * 顧客管理基盤 (#140) として、名前・メール・電話・ポイントを保持する。
 */
@Entity
@Table(name = "customers", schema = "store_schema")
class CustomerEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "points", nullable = false)
    var points: Long = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
