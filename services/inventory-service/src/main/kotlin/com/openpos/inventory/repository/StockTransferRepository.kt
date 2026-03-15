package com.openpos.inventory.repository

import com.openpos.inventory.entity.StockTransferEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StockTransferRepository : PanacheRepositoryBase<StockTransferEntity, UUID> {
    fun listPaginated(page: Page): List<StockTransferEntity> =
        findAll(Sort.descending("createdAt")).page(page).list()
}
