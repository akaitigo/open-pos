package com.openpos.inventory.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 棚卸し項目エンティティ。
 * 棚卸しの個別商品の実査結果を保持する。
 */
@Entity
@Table(name = "stocktake_items", schema = "inventory_schema")
class StocktakeItemEntity : BaseEntity() {
    @Column(name = "stocktake_id", nullable = false)
    lateinit var stocktakeId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "expected_qty", nullable = false)
    var expectedQty: Int = 0

    @Column(name = "actual_qty", nullable = false)
    var actualQty: Int = 0

    @Column(name = "difference", nullable = false)
    var difference: Int = 0
}
