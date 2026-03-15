package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.UUID

/**
 * 電子ジャーナルエンティティ。
 * POS 操作の不変監査ログ（SALE, VOID, RETURN, SETTLEMENT）を保持する。
 */
@Entity
@Table(name = "journal_entries", schema = "pos_schema")
class JournalEntryEntity : BaseEntity() {
    @Column(name = "type", nullable = false, length = 20)
    lateinit var type: String

    @Column(name = "transaction_id")
    var transactionId: UUID? = null

    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "terminal_id", nullable = false)
    lateinit var terminalId: UUID

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    var details: String = "{}"
}
