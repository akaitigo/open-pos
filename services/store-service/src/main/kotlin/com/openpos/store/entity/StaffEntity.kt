package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * スタッフエンティティ。
 * PIN 認証は POS 画面での素早いログインに使用する。
 * hydra_subject は ORY Hydra の OAuth2 サブジェクトとの紐付け。
 */
@Entity
@Table(name = "staff", schema = "store_schema")
class StaffEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "hydra_subject", length = 255, unique = true)
    var hydraSubject: String? = null

    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "role", nullable = false, length = 20)
    var role: String = "CASHIER"

    @Column(name = "pin_hash", length = 255)
    var pinHash: String? = null

    @Column(name = "pin_failed_count", nullable = false)
    var pinFailedCount: Int = 0

    @Column(name = "pin_locked_until")
    var pinLockedUntil: Instant? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
