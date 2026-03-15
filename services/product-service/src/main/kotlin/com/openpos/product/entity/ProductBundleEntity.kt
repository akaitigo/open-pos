package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * セット商品（バンドル販売）エンティティ。
 * セット商品 (#134)。
 * bundlePrice は銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "product_bundles", schema = "product_schema")
class ProductBundleEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "bundle_price", nullable = false)
    var bundlePrice: Long = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "description")
    var description: String? = null
}
