package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * 取引明細エンティティ。
 * 取引に含まれる個別商品の情報を保持する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持し、浮動小数点は使用しない。
 * taxRate は文字列（例: "0.10" = 10%）で保持する。
 */
@Entity
@Table(name = "transaction_items", schema = "pos_schema")
class TransactionItemEntity : BaseEntity() {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false, insertable = false, updatable = false)
    var transaction: TransactionEntity? = null

    @Column(name = "transaction_id", nullable = false)
    lateinit var transactionId: UUID

    @Column(name = "product_id", nullable = true)
    var productId: UUID? = null

    @Column(name = "product_name", nullable = false, length = 255)
    lateinit var productName: String

    @Column(name = "unit_price", nullable = false)
    var unitPrice: Long = 0

    @Column(name = "quantity", nullable = false)
    var quantity: Int = 1

    @Column(name = "tax_rate_name", nullable = false, length = 50)
    lateinit var taxRateName: String

    @Column(name = "tax_rate", nullable = false, length = 10)
    lateinit var taxRate: String

    @Column(name = "is_reduced_tax", nullable = false)
    var isReducedTax: Boolean = false

    @Column(name = "subtotal", nullable = false)
    var subtotal: Long = 0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long = 0

    @Column(name = "total", nullable = false)
    var total: Long = 0
}
