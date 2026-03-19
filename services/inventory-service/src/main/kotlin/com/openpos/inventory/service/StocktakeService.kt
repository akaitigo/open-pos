package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StocktakeEntity
import com.openpos.inventory.entity.StocktakeItemEntity
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.repository.StocktakeItemRepository
import com.openpos.inventory.repository.StocktakeRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 棚卸しサービス。
 * 棚卸しの開始、項目記録、完了（差異の在庫反映）を提供する。
 */
@ApplicationScoped
class StocktakeService {
    @Inject
    lateinit var stocktakeRepository: StocktakeRepository

    @Inject
    lateinit var stocktakeItemRepository: StocktakeItemRepository

    @Inject
    lateinit var stockRepository: StockRepository

    @Inject
    lateinit var stockService: StockService

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun startStocktake(storeId: UUID): StocktakeEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            StocktakeEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.status = "IN_PROGRESS"
                this.startedAt = Instant.now()
            }
        stocktakeRepository.persist(entity)
        return entity
    }

    @Transactional
    fun recordItem(
        stocktakeId: UUID,
        productId: UUID,
        actualQty: Int,
    ): StocktakeEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()

        val stocktake =
            stocktakeRepository.findById(stocktakeId)
                ?: throw IllegalArgumentException("Stocktake not found: $stocktakeId")
        require(stocktake.status == "IN_PROGRESS") { "Stocktake is not in progress" }

        val expectedQty =
            stockRepository.findByStoreAndProduct(stocktake.storeId, productId)?.quantity ?: 0

        val existing = stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId)
        if (existing != null) {
            existing.actualQty = actualQty
            existing.expectedQty = expectedQty
            existing.difference = actualQty - expectedQty
            stocktakeItemRepository.persist(existing)
        } else {
            val item =
                StocktakeItemEntity().apply {
                    this.organizationId = orgId
                    this.stocktakeId = stocktakeId
                    this.productId = productId
                    this.expectedQty = expectedQty
                    this.actualQty = actualQty
                    this.difference = actualQty - expectedQty
                }
            stocktakeItemRepository.persist(item)
        }
        return stocktake
    }

    @Transactional
    fun completeStocktake(stocktakeId: UUID): StocktakeEntity {
        tenantFilterService.enableFilter()
        val stocktake =
            stocktakeRepository.findById(stocktakeId)
                ?: throw IllegalArgumentException("Stocktake not found: $stocktakeId")
        require(stocktake.status == "IN_PROGRESS") { "Stocktake is not in progress" }

        val items = stocktakeItemRepository.findByStocktakeId(stocktakeId)
        for (item in items) {
            if (item.difference != 0) {
                stockService.adjustStock(
                    storeId = stocktake.storeId,
                    productId = item.productId,
                    quantityChange = item.difference,
                    movementType = "ADJUSTMENT",
                    referenceId = stocktakeId.toString(),
                    note = "棚卸し差異調整",
                )
            }
        }

        stocktake.status = "COMPLETED"
        stocktake.completedAt = Instant.now()
        stocktakeRepository.persist(stocktake)
        return stocktake
    }

    /**
     * EntityGraph を使って棚卸しと項目を一括取得する（N+1 防止）。
     */
    fun getStocktake(stocktakeId: UUID): StocktakeEntity {
        tenantFilterService.enableFilter()
        return stocktakeRepository.findByIdWithItems(stocktakeId)
            ?: throw IllegalArgumentException("Stocktake not found: $stocktakeId")
    }

    fun getStocktakeItems(stocktakeId: UUID): List<StocktakeItemEntity> = stocktakeItemRepository.findByStocktakeId(stocktakeId)
}
