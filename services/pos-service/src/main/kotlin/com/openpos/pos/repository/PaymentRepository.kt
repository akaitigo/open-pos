package com.openpos.pos.repository

import com.openpos.pos.entity.PaymentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class PaymentRepository : PanacheRepositoryBase<PaymentEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<PaymentEntity> = list("transactionId = ?1", transactionId)
}
