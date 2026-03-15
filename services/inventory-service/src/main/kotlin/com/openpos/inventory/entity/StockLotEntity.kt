package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 在庫ロットエンティティ。
 * 消費期限/賞味期限を管理するためにロット単位で在庫を追跡する。
 */
@Entity
@Table(name = "stock_lots", schema = "inventory_schema")
class StockLotEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "lot_number", length = 100)
    var lotNumber: String? = null

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Column(name = "expiry_date")
    var expiryDate: LocalDate? = null

    @Column(name = "received_at", nullable = false)
    var receivedAt: Instant = Instant.now()
}
