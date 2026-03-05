package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 在庫移動履歴エンティティ。
 * 全ての在庫変動を記録する（監査・分析に使用）。
 */
@Entity
@Table(name = "stock_movements", schema = "inventory_schema")
class StockMovementEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "movement_type", nullable = false, length = 20)
    lateinit var movementType: String

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Column(name = "reference_id", length = 36)
    var referenceId: String? = null

    @Column(name = "note", columnDefinition = "TEXT")
    var note: String? = null
}
