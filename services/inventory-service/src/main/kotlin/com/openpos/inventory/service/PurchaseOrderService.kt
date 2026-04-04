package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.PurchaseOrderEntity
import com.openpos.inventory.entity.PurchaseOrderItemEntity
import com.openpos.inventory.repository.PurchaseOrderItemRepository
import com.openpos.inventory.repository.PurchaseOrderRepository
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.OptimisticLockException
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 発注のビジネスロジック層。
 * 発注作成、取得、一覧、ステータス更新（DRAFT -> ORDERED -> RECEIVED / CANCELLED）を提供する。
 */
@ApplicationScoped
class PurchaseOrderService {
    @Inject
    lateinit var purchaseOrderRepository: PurchaseOrderRepository

    @Inject
    lateinit var itemRepository: PurchaseOrderItemRepository

    @Inject
    lateinit var stockService: StockService

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * 発注を作成する。
     */
    @Transactional
    fun create(
        storeId: UUID,
        supplierName: String,
        note: String?,
        items: List<PurchaseOrderItemInput>,
    ): PurchaseOrderEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        require(items.isNotEmpty()) { "At least one item is required" }
        // #924: storeId validation is performed at api-gateway layer via store-service gRPC
        // before forwarding to inventory-service. See InventoryResource.validateStoreExists().

        val order =
            PurchaseOrderEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.supplierName = supplierName
                this.note = note
                this.status = "DRAFT"
            }
        purchaseOrderRepository.persist(order)

        for (input in items) {
            val item =
                PurchaseOrderItemEntity().apply {
                    this.organizationId = orgId
                    this.purchaseOrderId = order.id
                    this.productId = input.productId
                    this.orderedQuantity = input.orderedQuantity
                    this.unitCost = input.unitCost
                }
            itemRepository.persist(item)
        }

        return order
    }

    /**
     * IDで発注を取得する。
     */
    fun findById(id: UUID): PurchaseOrderEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        return purchaseOrderRepository.find("id = ?1", id).firstResult()
    }

    /**
     * 発注の明細一覧を取得する。
     */
    fun getItems(purchaseOrderId: UUID): List<PurchaseOrderItemEntity> = itemRepository.findByPurchaseOrderId(purchaseOrderId)

    /**
     * 発注一覧を取得する（ページネーション対応）。
     */
    fun list(
        storeId: UUID,
        status: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<PurchaseOrderEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val orders = purchaseOrderRepository.listByStoreId(storeId, status, panachePage)
        val totalCount = purchaseOrderRepository.countByStoreId(storeId, status)
        return Pair(orders, totalCount)
    }

    /**
     * 発注ステータスを更新する。
     * RECEIVED への更新時は在庫を自動調整する。
     */
    @Transactional
    fun updateStatus(
        id: UUID,
        newStatus: String,
        receivedItems: List<ReceivedItemInput>,
    ): PurchaseOrderEntity {
        tenantFilterService.enableFilter()
        val order =
            purchaseOrderRepository.find("id = ?1", id).firstResult()
                ?: throw IllegalArgumentException("PurchaseOrder not found: $id")

        validateStatusTransition(order.status, newStatus)

        when (newStatus) {
            "ORDERED" -> {
                order.status = "ORDERED"
                order.orderedAt = Instant.now()
            }

            "RECEIVED" -> {
                order.status = "RECEIVED"
                order.receivedAt = Instant.now()
                processReceivedItems(order, receivedItems)
            }

            "CANCELLED" -> {
                order.status = "CANCELLED"
            }
        }

        try {
            purchaseOrderRepository.persist(order)
            purchaseOrderRepository.flush()
        } catch (e: OptimisticLockException) {
            throw StatusRuntimeException(
                Status.ABORTED.withDescription("Concurrent modification detected for PurchaseOrder: $id"),
            )
        }
        return order
    }

    private fun validateStatusTransition(
        current: String,
        target: String,
    ) {
        val valid =
            when (current) {
                "DRAFT" -> target in listOf("ORDERED", "CANCELLED")
                "ORDERED" -> target in listOf("RECEIVED", "CANCELLED")
                else -> false
            }
        require(valid) { "Invalid status transition: $current -> $target" }
    }

    private fun processReceivedItems(
        order: PurchaseOrderEntity,
        receivedItems: List<ReceivedItemInput>,
    ) {
        val items = itemRepository.findByPurchaseOrderId(order.id)
        val receivedMap = receivedItems.associateBy { it.productId }

        for (item in items) {
            val received = receivedMap[item.productId]
            item.receivedQuantity = received?.receivedQuantity ?: item.orderedQuantity
            itemRepository.persist(item)

            // 在庫に反映
            stockService.adjustStock(
                storeId = order.storeId,
                productId = item.productId,
                quantityChange = item.receivedQuantity,
                movementType = "RECEIPT",
                referenceId = order.id.toString(),
                note = "PO入荷: ${order.supplierName}",
            )
        }
    }
}

data class PurchaseOrderItemInput(
    val productId: UUID,
    val orderedQuantity: Int,
    val unitCost: Long,
)

data class ReceivedItemInput(
    val productId: UUID,
    val receivedQuantity: Int,
)
