package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 商品バリアントエンティティ。
 * 商品バリアント（サイズ・カラー） (#133)。
 * price は銭単位（10000 = 100円）。0 の場合は親商品の価格を使用。
 */
@Entity
@Table(name = "product_variants", schema = "product_schema")
class ProductVariantEntity : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    lateinit var productId: UUID

    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "sku", length = 100)
    var sku: String? = null

    @Column(name = "barcode", length = 100)
    var barcode: String? = null

    @Column(name = "price", nullable = false)
    var price: Long = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0
}
