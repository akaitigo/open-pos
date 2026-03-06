package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 取引割引エンティティ。
 * 取引全体またはトランザクション明細に対する割引を保持する。
 * transactionItemId が null の場合は取引全体への割引、
 * 非 null の場合は特定の明細への割引を示す。
 * amount は銭単位（BIGINT: 10000 = 100円）で保持する。
 */
@Entity
@Table(name = "transaction_discounts", schema = "pos_schema")
class TransactionDiscountEntity : BaseEntity() {
    @Column(name = "transaction_id", nullable = false)
    lateinit var transactionId: UUID

    @Column(name = "discount_id")
    var discountId: UUID? = null

    @Column(name = "name", nullable = false, length = 100)
    lateinit var name: String

    @Column(name = "discount_type", nullable = false, length = 20)
    lateinit var discountType: String

    @Column(name = "\"value\"", nullable = false, length = 50)
    lateinit var value: String

    @Column(name = "amount", nullable = false)
    var amount: Long = 0

    @Column(name = "transaction_item_id")
    var transactionItemId: UUID? = null
}
