package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 精算（レジ締め）エンティティ。
 * レジ締め時の現金差異（過不足）を記録する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持する。
 */
@Entity
@Table(name = "settlements", schema = "pos_schema")
class SettlementEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "terminal_id", nullable = false)
    lateinit var terminalId: UUID

    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "cash_expected", nullable = false)
    var cashExpected: Long = 0

    @Column(name = "cash_actual", nullable = false)
    var cashActual: Long = 0

    @Column(name = "difference", nullable = false)
    var difference: Long = 0

    @Column(name = "settled_at", nullable = false)
    var settledAt: Instant = Instant.now()
}
