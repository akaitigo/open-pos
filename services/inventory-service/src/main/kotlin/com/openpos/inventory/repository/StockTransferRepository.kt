package com.openpos.inventory.repository

import com.openpos.inventory.entity.StockTransferEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StockTransferRepository : PanacheRepositoryBase<StockTransferEntity, UUID> {
    fun listPaginated(page: Page): List<StockTransferEntity> = findAll(Sort.descending("createdAt")).page(page).list()

    fun listByStoreId(
        storeId: UUID,
        status: String?,
        page: Page,
    ): List<StockTransferEntity> {
        val query =
            if (status != null) {
                find(
                    "(fromStoreId = ?1 or toStoreId = ?1) and status = ?2",
                    Sort.descending("createdAt"),
                    storeId,
                    status,
                )
            } else {
                find(
                    "fromStoreId = ?1 or toStoreId = ?1",
                    Sort.descending("createdAt"),
                    storeId,
                )
            }
        return query.page(page).list()
    }

    fun countByStoreId(
        storeId: UUID,
        status: String?,
    ): Long =
        if (status != null) {
            count("(fromStoreId = ?1 or toStoreId = ?1) and status = ?2", storeId, status)
        } else {
            count("fromStoreId = ?1 or toStoreId = ?1", storeId)
        }
}
