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
 * サブスクリプションエンティティ。
 * テナント（組織）のプラン契約状態を管理する。
 * ステータス遷移: TRIAL -> ACTIVE -> CANCELLED / EXPIRED
 */
@Entity
@Table(name = "subscriptions", schema = "store_schema")
class SubscriptionEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "organization_id", nullable = false)
    lateinit var organizationId: UUID

    @Column(name = "plan_id", nullable = false)
    lateinit var planId: UUID

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"

    @Column(name = "start_date", nullable = false)
    var startDate: Instant = Instant.now()

    @Column(name = "end_date")
    var endDate: Instant? = null

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
