package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * 発注エンティティ。
 * サプライヤーへの発注を管理する。
 * ステータス遷移: DRAFT -> ORDERED -> RECEIVED / CANCELLED
 */
@Entity
@Table(name = "purchase_orders", schema = "inventory_schema")
class PurchaseOrderEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "DRAFT"

    @Column(name = "supplier_name", nullable = false, length = 255)
    lateinit var supplierName: String

    @Column(name = "note")
    var note: String? = null

    @Column(name = "ordered_at")
    var orderedAt: Instant? = null

    @Column(name = "received_at")
    var receivedAt: Instant? = null

    /**
     * 楽観的ロック用バージョン番号。
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
}
