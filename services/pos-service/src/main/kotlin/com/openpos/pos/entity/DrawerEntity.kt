package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * ドロワー（つり銭準備金）エンティティ。
 * 端末ごとのレジドロワーの開閉状態と現金残高を管理する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持する。
 */
@Entity
@Table(name = "drawers", schema = "pos_schema")
class DrawerEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "terminal_id", nullable = false)
    lateinit var terminalId: UUID

    @Column(name = "opening_amount", nullable = false)
    var openingAmount: Long = 0

    @Column(name = "current_amount", nullable = false)
    var currentAmount: Long = 0

    @Column(name = "is_open", nullable = false)
    var isOpen: Boolean = false

    @Column(name = "opened_at")
    var openedAt: Instant? = null

    @Column(name = "closed_at")
    var closedAt: Instant? = null
}
