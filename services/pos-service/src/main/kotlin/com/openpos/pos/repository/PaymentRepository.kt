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
}
