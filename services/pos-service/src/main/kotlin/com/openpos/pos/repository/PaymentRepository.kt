package com.openpos.pos.repository

import com.openpos.pos.entity.PaymentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class PaymentRepository : PanacheRepositoryBase<PaymentEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<PaymentEntity> = list("transactionId = ?1", transactionId)

    fun sumCashPaymentsByTerminal(
        storeId: UUID,
        terminalId: UUID,
    ): Long {
        val result =
            getEntityManager()
                .createQuery(
                    """
                    SELECT COALESCE(SUM(p.amount), 0)
                    FROM PaymentEntity p
                    JOIN TransactionEntity t ON p.transactionId = t.id
                    WHERE t.storeId = :storeId
                      AND t.terminalId = :terminalId
                      AND t.status = 'COMPLETED'
                      AND p.method = 'CASH'
                    """.trimIndent(),
                    Long::class.javaObjectType,
                ).setParameter("storeId", storeId)
                .setParameter("terminalId", terminalId)
                .singleResult
        return result ?: 0L
    }
}
