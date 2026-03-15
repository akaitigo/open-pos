package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * ギフトカード/プリペイドカードエンティティ。
 * ギフトカード管理 (#142)。
 * balance は銭単位（10000 = 100円）。
 */
@Entity
@Table(name = "gift_cards", schema = "store_schema")
class GiftCardEntity : BaseEntity() {
    @Column(name = "code", nullable = false, unique = true, length = 50)
    lateinit var code: String

    @Column(name = "balance", nullable = false)
    var balance: Long = 0

    @Column(name = "initial_balance", nullable = false)
    var initialBalance: Long = 0

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"
}
