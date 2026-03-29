package com.openpos.store.entity

import com.openpos.store.entity.annotation.PersonalData
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
    /** @PII 顧客氏名 */
    @PersonalData(category = "NAME", description = "顧客氏名")
    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    /** @PII メールアドレス */
    @PersonalData(category = "EMAIL", description = "顧客メールアドレス")
    @Column(name = "email", length = 255)
    var email: String? = null

    /** @PII 電話番号 */
    @PersonalData(category = "PHONE", description = "顧客電話番号")
    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "points", nullable = false)
    var points: Long = 0

    /** 顧客ランク: REGULAR, SILVER, GOLD, VIP */
    @Column(name = "tier", nullable = false, length = 20)
    var tier: String = "REGULAR"

    /** 顧客メモ（店舗スタッフ向けの自由記述） */
    @Column(name = "notes", columnDefinition = "TEXT")
    var notes: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
