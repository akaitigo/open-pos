package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * 商品バリアントエンティティ。
 * 商品バリアント（サイズ・カラー） (#133)。
 * price は銭単位、attributes は JSON で属性情報を保持。
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

    @Column(name = "price", nullable = false)
    var price: Long = 0

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "attributes", nullable = false, columnDefinition = "jsonb")
    var attributes: String = "{}"

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
