package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.SQLDelete
import org.hibernate.annotations.SQLRestriction
import java.util.UUID

/**
 * 決済エンティティ。
 * 取引に対する支払い方法と金額を保持する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持し、浮動小数点は使用しない。
 * 現金払いの場合、received（受領額）と change（お釣り）を記録する。
 * 監査ログ完全性のため論理削除を採用（物理削除禁止）。
 */
@Entity
@Table(name = "payments", schema = "pos_schema")
@SQLDelete(sql = "UPDATE pos_schema.payments SET deleted = true WHERE id = ?")
@SQLRestriction("deleted = false")
class PaymentEntity : BaseEntity() {
    @Column(name = "transaction_id", nullable = false)
    lateinit var transactionId: UUID

    @Column(name = "method", nullable = false, length = 20)
    lateinit var method: String

    @Column(name = "amount", nullable = false)
    var amount: Long = 0

    @Column(name = "received")
    var received: Long? = null

    @Column(name = "\"change\"")
    var change: Long? = null

    @Column(name = "reference", length = 255)
    var reference: String? = null

    /**
     * 論理削除フラグ。
     * 監査ログの完全性を保つため、決済レコードは物理削除せず論理削除する。
     */
    @Column(name = "deleted", nullable = false)
    var deleted: Boolean = false
}
