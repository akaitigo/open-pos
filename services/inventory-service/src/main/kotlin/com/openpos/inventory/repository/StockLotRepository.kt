package com.openpos.inventory.repository

import com.openpos.inventory.entity.StockLotEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class StockLotRepository : PanacheRepositoryBase<StockLotEntity, UUID> {
    fun listByStoreAndProduct(
        storeId: UUID,
        productId: UUID,
        page: Page,
    ): List<StockLotEntity> =
        find("storeId = ?1 AND productId = ?2", Sort.ascending("expiryDate"), storeId, productId)
            .page(page)
            .list()

    fun countByStoreAndProduct(
        storeId: UUID,
        productId: UUID,
    ): Long = count("storeId = ?1 AND productId = ?2", storeId, productId)

    fun findExpiringSoon(
        daysAhead: Int,
        page: Page,
    ): List<StockLotEntity> {
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong())
        return find(
            "expiryDate IS NOT NULL AND expiryDate <= ?1 AND quantity > 0",
            Sort.ascending("expiryDate"),
            targetDate,
        ).page(page).list()
    }

    fun countExpiringSoon(daysAhead: Int): Long {
        val targetDate = LocalDate.now().plusDays(daysAhead.toLong())
        return count("expiryDate IS NOT NULL AND expiryDate <= ?1 AND quantity > 0", targetDate)
    }
}
