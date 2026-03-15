package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.util.UUID

/**
 * 在庫エンティティ。
 * 店舗×商品の現在在庫数を保持する。
 * organization_id + store_id + product_id でユニーク制約。
 */
@Entity
@Table(name = "stocks", schema = "inventory_schema")
class StockEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 0

    @Column(name = "low_stock_threshold", nullable = false)
    var lowStockThreshold: Int = 10

    @Column(name = "reorder_point", nullable = false)
    var reorderPoint: Int = 0

    @Column(name = "reorder_quantity", nullable = false)
    var reorderQuantity: Int = 0

    /**
     * 楽観的ロック用バージョン番号。
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
}
