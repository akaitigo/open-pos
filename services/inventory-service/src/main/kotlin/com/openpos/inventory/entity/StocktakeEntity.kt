package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 棚卸しエンティティ。
 * 在庫実査のヘッダー情報を保持する。
 */
@Entity
@Table(name = "stocktakes", schema = "inventory_schema")
class StocktakeEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "IN_PROGRESS"

    @Column(name = "started_at", nullable = false)
    var startedAt: Instant = Instant.now()

    @Column(name = "completed_at")
    var completedAt: Instant? = null
}
