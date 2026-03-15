package com.openpos.inventory.repository

import com.openpos.inventory.entity.StocktakeItemEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StocktakeItemRepository : PanacheRepositoryBase<StocktakeItemEntity, UUID> {
    fun findByStocktakeId(stocktakeId: UUID): List<StocktakeItemEntity> = find("stocktakeId = ?1", stocktakeId).list()

    fun findByStocktakeAndProduct(
        stocktakeId: UUID,
        productId: UUID,
    ): StocktakeItemEntity? = find("stocktakeId = ?1 and productId = ?2", stocktakeId, productId).firstResult()
}
