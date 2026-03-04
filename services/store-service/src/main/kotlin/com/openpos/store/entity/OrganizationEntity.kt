package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 組織エンティティ（テナントルート）。
 * organizations テーブルは organization_id を持たないため BaseEntity を継承しない。
 */
@Entity
@Table(name = "organizations", schema = "store_schema")
class OrganizationEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "business_type", nullable = false, length = 20)
    var businessType: String = "RETAIL"

    @Column(name = "invoice_number", length = 20, unique = true)
    var invoiceNumber: String? = null

    @Column(name = "plan", nullable = false, length = 20)
    var plan: String = "FREE"

    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
