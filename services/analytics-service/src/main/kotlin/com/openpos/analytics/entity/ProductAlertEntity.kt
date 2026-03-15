package com.openpos.analytics.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 商品アラートエンティティ。
 * 売れ筋変動・在庫異常を検出して記録する。
 * alertType: TRENDING（急上昇）、DECLINING（急降下）、ANOMALY（異常検知）
 */
@Entity
@Table(name = "product_alerts", schema = "analytics_schema")
class ProductAlertEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "organization_id", nullable = false)
    lateinit var organizationId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "alert_type", nullable = false, length = 20)
    lateinit var alertType: String

    @Column(name = "description", nullable = false)
    lateinit var description: String

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
    }
}
