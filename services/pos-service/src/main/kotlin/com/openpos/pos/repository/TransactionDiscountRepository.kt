package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionDiscountEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class TransactionDiscountRepository : PanacheRepositoryBase<TransactionDiscountEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<TransactionDiscountEntity> = list("transactionId = ?1", transactionId)

    /**
     * 複数取引IDに対する割引を一括取得する（N+1 防止）。
     */
    fun findByTransactionIds(transactionIds: List<UUID>): List<TransactionDiscountEntity> {
        if (transactionIds.isEmpty()) return emptyList()
        return list("transactionId IN ?1", transactionIds)
    }
}
