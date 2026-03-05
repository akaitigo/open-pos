package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.util.UUID

/**
 * 税額集計エンティティ。
 * 取引内の税率ごとの課税対象額と税額を集計する。
 * インボイス対応: 標準税率と軽減税率を区別して記録する。
 * 金額は全て銭単位（BIGINT: 10000 = 100円）で保持する。
 */
@Entity
@Table(name = "tax_summaries", schema = "pos_schema")
class TaxSummaryEntity : BaseEntity() {
    @Column(name = "transaction_id", nullable = false)
    lateinit var transactionId: UUID

    @Column(name = "tax_rate_name", nullable = false, length = 50)
    lateinit var taxRateName: String

    @Column(name = "tax_rate", nullable = false, length = 10)
    lateinit var taxRate: String

    @Column(name = "is_reduced", nullable = false)
    var isReduced: Boolean = false

    @Column(name = "taxable_amount", nullable = false)
    var taxableAmount: Long = 0

    @Column(name = "tax_amount", nullable = false)
    var taxAmount: Long = 0
}
