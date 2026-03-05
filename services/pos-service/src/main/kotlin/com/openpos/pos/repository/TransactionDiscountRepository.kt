package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionDiscountEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class TransactionDiscountRepository : PanacheRepositoryBase<TransactionDiscountEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<TransactionDiscountEntity> = list("transactionId = ?1", transactionId)
}
