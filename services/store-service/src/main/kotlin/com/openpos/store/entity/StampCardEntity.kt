package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * スタンプカードエンティティ。
 * 顧客ロイヤリティプログラムのスタンプ式カード。
 * status: ACTIVE（利用中）、COMPLETED（最大到達・報酬交換待ち）、EXPIRED（期限切れ）
 */
@Entity
@Table(
    name = "stamp_cards",
    schema = "store_schema",
    indexes = [
        Index(name = "idx_stamp_cards_org", columnList = "organization_id"),
        Index(name = "idx_stamp_cards_customer", columnList = "organization_id, customer_id"),
    ],
)
class StampCardEntity : BaseEntity() {
    @Column(name = "customer_id", nullable = false)
    lateinit var customerId: UUID

    @Column(name = "stamp_count", nullable = false)
    var stampCount: Int = 0

    @Column(name = "max_stamps", nullable = false)
    var maxStamps: Int = 10

    @Column(name = "reward_description")
    var rewardDescription: String? = null

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "ACTIVE"

    @Column(name = "issued_at", nullable = false)
    var issuedAt: Instant = Instant.now()
}
