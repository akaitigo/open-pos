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

    @Inject lateinit var stockService: StockService

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

    fun listByStoreId(
        storeId: UUID,
        status: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<StockTransferEntity>, Long> {
        tenantFilterService.enableFilter()
        val items = stockTransferRepository.listByStoreId(storeId, status, Page.of(page, pageSize))
        val total = stockTransferRepository.countByStoreId(storeId, status)
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

    /**
     * 在庫移動を完了する。
     * ステータスを COMPLETED に変更し、移動元店舗から在庫を減少、移動先店舗に在庫を増加させる。
     */
    @Transactional
    fun complete(id: UUID): StockTransferEntity {
        tenantFilterService.enableFilter()
        val entity =
            stockTransferRepository.findById(id)
                ?: throw IllegalArgumentException("StockTransfer not found: $id")
        require(entity.status == "PENDING" || entity.status == "IN_TRANSIT") {
            "Cannot complete transfer with status: ${entity.status}"
        }

        val transferItems = parseItems(entity.items)
        for (item in transferItems) {
            stockService.adjustStock(
                storeId = entity.fromStoreId,
                productId = item.productId,
                quantityChange = -item.quantity,
                movementType = "TRANSFER",
                referenceId = entity.id.toString(),
                note = "Stock transfer to store ${entity.toStoreId}",
            )
            stockService.adjustStock(
                storeId = entity.toStoreId,
                productId = item.productId,
                quantityChange = item.quantity,
                movementType = "TRANSFER",
                referenceId = entity.id.toString(),
                note = "Stock transfer from store ${entity.fromStoreId}",
            )
        }

        entity.status = "COMPLETED"
        stockTransferRepository.persist(entity)
        return entity
    }

    internal fun parseItems(json: String): List<TransferItem> {
        val items = mutableListOf<TransferItem>()
        val regex = Regex(""""productId"\s*:\s*"([^"]+)"\s*,\s*"quantity"\s*:\s*(\d+)""")
        for (match in regex.findAll(json)) {
            items.add(
                TransferItem(
                    productId = UUID.fromString(match.groupValues[1]),
                    quantity = match.groupValues[2].toInt(),
                ),
            )
        }
        return items
    }
}

data class TransferItem(
    val productId: UUID,
    val quantity: Int,
)
