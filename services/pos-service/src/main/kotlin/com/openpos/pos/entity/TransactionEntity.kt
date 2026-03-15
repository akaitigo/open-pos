package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.util.UUID

/**
 * 取引エンティティ。
 * POS端末での販売・返品・取消の取引情報を保持する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持し、浮動小数点は使用しない。
 */
@Entity
@Table(name = "transactions", schema = "pos_schema")
class TransactionEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "terminal_id", nullable = false)
    lateinit var terminalId: UUID

    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "transaction_number", nullable = false, length = 30)
    lateinit var transactionNumber: String

    @Column(name = "type", nullable = false, length = 20)
    var type: String = "SALE"

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "DRAFT"

    @Column(name = "client_id", length = 36)
    var clientId: String? = null

    @Column(name = "subtotal", nullable = false)
    var subtotal: Long = 0

    @Column(name = "tax_total", nullable = false)
    var taxTotal: Long = 0

    @Column(name = "discount_total", nullable = false)
    var discountTotal: Long = 0

    @Column(name = "total", nullable = false)
    var total: Long = 0

    @Column(name = "table_number", length = 20)
    var tableNumber: String? = null

    @Column(name = "completed_at")
    var completedAt: Instant? = null

    /**
     * 楽観的ロック用バージョン番号。
     */
    @Version
    @Column(name = "version", nullable = false)
    var version: Long = 0
}
