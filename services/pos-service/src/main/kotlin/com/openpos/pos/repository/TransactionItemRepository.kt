package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionItemEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 取引明細リポジトリ。
 */
@ApplicationScoped
class TransactionItemRepository : PanacheRepositoryBase<TransactionItemEntity, UUID> {
    /**
     * 取引IDで明細一覧を取得する。
     */
    fun findByTransactionId(transactionId: UUID): List<TransactionItemEntity> = list("transactionId = ?1", transactionId)
}
