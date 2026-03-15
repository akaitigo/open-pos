package com.openpos.pos.repository

import com.openpos.pos.entity.PaymentEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class PaymentRepository : PanacheRepositoryBase<PaymentEntity, UUID> {
    fun findByTransactionId(transactionId: UUID): List<PaymentEntity> = list("transactionId = ?1", transactionId)

    /**
     * 複数取引IDに対する決済を一括取得する（N+1 防止）。
     */
    fun findByTransactionIds(transactionIds: List<UUID>): List<PaymentEntity> {
        if (transactionIds.isEmpty()) return emptyList()
        return list("transactionId IN ?1", transactionIds)
    }

    /**
     * 指定端末の完了済み取引における現金支払合計を取得する（精算用）。
     */
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
