package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.ParamDef
import java.time.Instant
import java.util.UUID

/**
 * 商品エンティティ。
 * 販売可能な商品のマスタ情報を保持する。
 * price は銭単位（BIGINT: 10000 = 100円）で保持し、浮動小数点は使用しない。
 */
@Entity
@Table(name = "products", schema = "product_schema")
@FilterDef(name = "activeProductFilter", defaultCondition = "deleted_at IS NULL")
@Filter(name = "activeProductFilter")
class ProductEntity : BaseEntity() {
    @Column(name = "category_id")
    var categoryId: UUID? = null

    @Column(name = "tax_rate_id")
    var taxRateId: UUID? = null

    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "description")
    var description: String? = null

    @Column(name = "barcode", length = 100)
    var barcode: String? = null

    @Column(name = "sku", length = 100)
    var sku: String? = null

    @Column(name = "price", nullable = false)
    var price: Long = 0

    @Column(name = "image_url")
    var imageUrl: String? = null

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true

    /**
     * 論理削除日時。null の場合はアクティブな商品。
     */
    @Column(name = "deleted_at")
    var deletedAt: Instant? = null

    /**
     * 楽観的ロック用バージョン番号。
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
}
