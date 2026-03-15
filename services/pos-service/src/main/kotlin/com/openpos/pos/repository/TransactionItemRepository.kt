package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionItemEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class TransactionItemRepository : PanacheRepositoryBase<TransactionItemEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<TransactionItemEntity> = list("transactionId = ?1", transactionId)

    fun findByTransactionAndProduct(
        transactionId: UUID,
        productId: UUID,
    ): TransactionItemEntity? = find("transactionId = ?1 and productId = ?2", transactionId, productId).firstResult()

    /**
     * 複数取引IDに対する明細を一括取得する（N+1 防止）。
     */
    fun findByTransactionIds(transactionIds: List<UUID>): List<TransactionItemEntity> {
        if (transactionIds.isEmpty()) return emptyList()
        return list("transactionId IN ?1", transactionIds)
    }
}
