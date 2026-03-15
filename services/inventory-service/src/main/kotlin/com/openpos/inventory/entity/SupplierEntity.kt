package com.openpos.inventory.entity

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

    @Column(name = "contact_person", length = 100)
    var contactPerson: String? = null

    @Column(name = "email", length = 255)
    var email: String? = null

    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "address", columnDefinition = "text")
    var address: String? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
