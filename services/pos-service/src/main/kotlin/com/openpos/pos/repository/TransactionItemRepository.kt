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
}
