package com.openpos.store.entity

import com.fasterxml.jackson.annotation.JsonIgnore
import com.openpos.store.entity.annotation.PersonalData
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

    /** @PII OAuth2 サブジェクト識別子 */
    @PersonalData(category = "AUTH_CREDENTIAL", description = "ORY Hydra OAuth2 サブジェクト")
    @Column(name = "hydra_subject", length = 255, unique = true)
    var hydraSubject: String? = null

    /** @PII スタッフ氏名 */
    @PersonalData(category = "NAME", description = "スタッフ表示名")
    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    /** @PII メールアドレス */
    @PersonalData(category = "EMAIL", description = "スタッフメールアドレス")
    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "role", nullable = false, length = 20)
    var role: String = "CASHIER"

    /** @PII PIN ハッシュ（認証情報） */
    @PersonalData(category = "AUTH_CREDENTIAL", description = "PIN 認証ハッシュ")
    @JsonIgnore
    @Column(name = "pin_hash", length = 255)
    var pinHash: String? = null

    @Column(name = "pin_failed_count", nullable = false)
    var pinFailedCount: Int = 0

    @Column(name = "pin_locked_until")
    var pinLockedUntil: Instant? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
