package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 発注明細エンティティ。
 * 発注した商品と数量、入荷確認数量を保持する。
 */
@Entity
@Table(name = "purchase_order_items", schema = "inventory_schema")
class PurchaseOrderItemEntity : BaseEntity() {
    @Column(name = "purchase_order_id", nullable = false)
    lateinit var purchaseOrderId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "ordered_quantity", nullable = false)
    var orderedQuantity: Int = 0

    @Column(name = "received_quantity", nullable = false)
    var receivedQuantity: Int = 0

    @Column(name = "unit_cost", nullable = false)
    var unitCost: Long = 0
}
