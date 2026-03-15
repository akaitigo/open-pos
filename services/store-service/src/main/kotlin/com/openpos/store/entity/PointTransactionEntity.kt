package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * ポイント取引エンティティ。
 * ポイント/ロイヤルティシステム (#141)。
 * 100円につき1ポイント付与、銭単位計算。
 */
@Entity
@Table(name = "point_transactions", schema = "store_schema")
class PointTransactionEntity : BaseEntity() {
    @Column(name = "customer_id", nullable = false)
    lateinit var customerId: UUID

    @Column(name = "points", nullable = false)
    var points: Long = 0

    @Column(name = "type", nullable = false, length = 20)
    var type: String = "EARN"

    @Column(name = "transaction_id")
    var transactionId: UUID? = null

    @Column(name = "description")
    var description: String? = null
}
