package com.openpos.inventory.entity

import com.openpos.inventory.entity.annotation.PersonalData
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 仕入先エンティティ。
 * 仕入先管理 (#144)。
 */
@Entity
@Table(name = "suppliers", schema = "inventory_schema")
class SupplierEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    /** @PII 担当者名 */
    @PersonalData(category = "NAME", description = "仕入先担当者名")
    @Column(name = "contact_person", length = 100)
    var contactPerson: String? = null

    /** @PII 仕入先メールアドレス */
    @PersonalData(category = "EMAIL", description = "仕入先メールアドレス")
    @Column(name = "email", length = 255)
    var email: String? = null

    /** @PII 仕入先電話番号 */
    @PersonalData(category = "PHONE", description = "仕入先電話番号")
    @Column(name = "phone", length = 20)
    var phone: String? = null

    /** @PII 仕入先住所 */
    @PersonalData(category = "ADDRESS", description = "仕入先住所")
    @Column(name = "address", columnDefinition = "text")
    var address: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
