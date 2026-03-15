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
 * プランエンティティ。
 * マルチテナントのプランマスタ。テナント横断なので BaseEntity を継承しない。
 * 月額料金は銭単位（BIGINT: 10000 = 100円）。
 */
@Entity
@Table(name = "plans", schema = "store_schema")
class PlanEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    @Column(name = "max_stores", nullable = false)
    var maxStores: Int = 1

    @Column(name = "max_terminals", nullable = false)
    var maxTerminals: Int = 2

    @Column(name = "max_products", nullable = false)
    var maxProducts: Int = 100

    @Column(name = "monthly_price", nullable = false)
    var monthlyPrice: Long = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

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
