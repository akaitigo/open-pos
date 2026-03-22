package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockTransferEntity
import com.openpos.inventory.repository.StockTransferRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class StockTransferService {
    @Inject lateinit var stockTransferRepository: StockTransferRepository

    @Inject lateinit var tenantFilterService: TenantFilterService

    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(
        fromStoreId: UUID,
        toStoreId: UUID,
        items: String,
        note: String?,
    ): StockTransferEntity {
        require(fromStoreId != toStoreId) { "fromStoreId and toStoreId must be different" }
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            StockTransferEntity().apply {
                this.organizationId = orgId
                this.fromStoreId = fromStoreId
                this.toStoreId = toStoreId
                this.items = items
                this.note = note
                this.status = "PENDING"
            }
        stockTransferRepository.persist(entity)
        return entity
    }

    fun findById(id: UUID): StockTransferEntity? {
        tenantFilterService.enableFilter()
        return stockTransferRepository.findById(id)
    }

    fun list(
        page: Int,
        pageSize: Int,
    ): Pair<List<StockTransferEntity>, Long> {
        tenantFilterService.enableFilter()
        val items = stockTransferRepository.listPaginated(Page.of(page, pageSize))
        val total = stockTransferRepository.count()
        return Pair(items, total)
    }

    @Transactional
    fun updateStatus(
        id: UUID,
        status: String,
    ): StockTransferEntity? {
        tenantFilterService.enableFilter()
        val entity = stockTransferRepository.findById(id) ?: return null
        entity.status = status
        stockTransferRepository.persist(entity)
        return entity
    }
}
