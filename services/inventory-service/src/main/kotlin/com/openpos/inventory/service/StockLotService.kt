package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockLotEntity
import com.openpos.inventory.repository.StockLotRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 在庫ロットサービス。
 * 消費期限/賞味期限管理のためロット単位での在庫操作を提供する。
 */
@ApplicationScoped
class StockLotService {
    @Inject
    lateinit var stockLotRepository: StockLotRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listByStoreAndProduct(
        storeId: UUID,
        productId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockLotEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val lots = stockLotRepository.listByStoreAndProduct(storeId, productId, panachePage)
        val count = stockLotRepository.countByStoreAndProduct(storeId, productId)
        return Pair(lots, count)
    }

    fun listExpiringSoon(
        daysAhead: Int,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockLotEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val lots = stockLotRepository.findExpiringSoon(daysAhead, panachePage)
        val count = stockLotRepository.countExpiringSoon(daysAhead)
        return Pair(lots, count)
    }

    @Transactional
    fun create(
        storeId: UUID,
        productId: UUID,
        lotNumber: String?,
        quantity: Int,
        expiryDate: LocalDate?,
    ): StockLotEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            StockLotEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.productId = productId
                this.lotNumber = lotNumber
                this.quantity = quantity
                this.expiryDate = expiryDate
                this.receivedAt = Instant.now()
            }
        stockLotRepository.persist(entity)
        return entity
    }
}
