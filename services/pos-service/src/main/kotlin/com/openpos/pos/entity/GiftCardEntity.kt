package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

/**
 * ギフトカードエンティティ。
 * プリペイド方式のストアバリューカードを管理する。
 * 金額はすべて銭単位（10000 = 100円）。
 * status: PENDING（発行済み未有効化）、ACTIVE（有効）、DEPLETED（残高0）、EXPIRED（期限切れ）、CANCELLED（キャンセル）
 */
@Entity
@Table(
    name = "gift_cards",
    schema = "pos_schema",
    indexes = [
        Index(name = "idx_gift_cards_org", columnList = "organization_id"),
        Index(name = "idx_gift_cards_code", columnList = "organization_id, code", unique = true),
    ],
)
class GiftCardEntity : BaseEntity() {
    @Column(name = "code", nullable = false, length = 19)
    lateinit var code: String

    @Column(name = "initial_amount", nullable = false)
    var initialAmount: Long = 0

    @Column(name = "balance", nullable = false)
    var balance: Long = 0

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "PENDING"

    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant = Instant.now()

    @Column(name = "expires_at")
    var expiresAt: Instant? = null
}
