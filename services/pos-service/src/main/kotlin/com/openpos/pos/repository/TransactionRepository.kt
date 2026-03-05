package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 取引リポジトリ。
 * クライアントID検索、店舗別一覧取得をサポートする。
 */
@ApplicationScoped
class TransactionRepository : PanacheRepositoryBase<TransactionEntity, UUID> {
    /**
     * クライアントIDで取引を検索する（オフライン取引の同期用）。
     */
    fun findByClientId(clientId: String): TransactionEntity? = find("clientId = ?1", clientId).firstResult()

    /**
     * 店舗IDで取引一覧を取得する（新しい順）。
     */
    fun listByStoreId(
        storeId: UUID,
        page: Page,
    ): List<TransactionEntity> =
        find("storeId = ?1", Sort.descending("createdAt"), storeId)
            .page(page)
            .list()
}
