package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * セット商品明細エンティティ。
 * バンドル販売の個別商品と数量 (#134)。
 */
@Entity
@Table(name = "product_bundle_items", schema = "product_schema")
class ProductBundleItemEntity : BaseEntity() {
    @Column(name = "bundle_id", nullable = false)
    lateinit var bundleId: UUID

    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1
}
