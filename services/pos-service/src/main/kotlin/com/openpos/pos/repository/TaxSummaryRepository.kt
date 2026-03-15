package com.openpos.pos.repository

import com.openpos.pos.entity.TaxSummaryEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class TaxSummaryRepository : PanacheRepositoryBase<TaxSummaryEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<TaxSummaryEntity> = list("transactionId = ?1", transactionId)

    fun deleteByTransactionId(transactionId: UUID): Long = delete("transactionId = ?1", transactionId)

    /**
     * 複数取引IDに対する税率集計を一括取得する（N+1 防止）。
     */
    fun findByTransactionIds(transactionIds: List<UUID>): List<TaxSummaryEntity> {
        if (transactionIds.isEmpty()) return emptyList()
        return list("transactionId IN ?1", transactionIds)
    }
}
