package com.openpos.product.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * レシートテンプレートエンティティ。
 * レシートカスタマイズ (#149)。
 */
@Entity
@Table(name = "receipt_templates", schema = "product_schema")
class ReceiptTemplateEntity : BaseEntity() {
    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    @Column(name = "header", columnDefinition = "text")
    var header: String? = null

    @Column(name = "footer", columnDefinition = "text")
    var footer: String? = null

    @Column(name = "logo_url")
    var logoUrl: String? = null

    @Column(name = "show_barcode", nullable = false)
    var showBarcode: Boolean = true

    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false
}
