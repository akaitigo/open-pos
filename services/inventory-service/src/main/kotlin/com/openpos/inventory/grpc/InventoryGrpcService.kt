package com.openpos.inventory.grpc

import com.openpos.inventory.entity.PurchaseOrderEntity
import com.openpos.inventory.entity.PurchaseOrderItemEntity
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StockMovementEntity
import com.openpos.inventory.service.PurchaseOrderItemInput
import com.openpos.inventory.service.PurchaseOrderService
import com.openpos.inventory.service.ReceivedItemInput
import com.openpos.inventory.service.StockService
import io.grpc.Status
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.persistence.OptimisticLockException
import openpos.common.v1.PaginationResponse
import openpos.inventory.v1.AdjustStockRequest
import openpos.inventory.v1.AdjustStockResponse
import openpos.inventory.v1.CreatePurchaseOrderRequest
import openpos.inventory.v1.CreatePurchaseOrderResponse
import openpos.inventory.v1.GetPurchaseOrderRequest
import openpos.inventory.v1.GetPurchaseOrderResponse
import openpos.inventory.v1.GetStockRequest
import openpos.inventory.v1.GetStockResponse
import openpos.inventory.v1.InventoryServiceGrpc
import openpos.inventory.v1.ListPurchaseOrdersRequest
import openpos.inventory.v1.ListPurchaseOrdersResponse
import openpos.inventory.v1.ListStockMovementsRequest
import openpos.inventory.v1.ListStockMovementsResponse
import openpos.inventory.v1.ListStocksRequest
import openpos.inventory.v1.ListStocksResponse
import openpos.inventory.v1.MovementType
import openpos.inventory.v1.PurchaseOrder
import openpos.inventory.v1.PurchaseOrderItem
import openpos.inventory.v1.PurchaseOrderStatus
import openpos.inventory.v1.Stock
import openpos.inventory.v1.StockMovement
import openpos.inventory.v1.UpdatePurchaseOrderStatusRequest
import openpos.inventory.v1.UpdatePurchaseOrderStatusResponse
import java.time.Instant
import java.util.UUID

@GrpcService
@Blocking
class InventoryGrpcService : InventoryServiceGrpc.InventoryServiceImplBase() {
    @Inject
    lateinit var stockService: StockService

    @Inject
    lateinit var purchaseOrderService: PurchaseOrderService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    // === Stock ===

    override fun getStock(
        request: GetStockRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetStockResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val entity =
            stockService.getStock(
                storeId = request.storeId.toUUID(),
                productId = request.productId.toUUID(),
            ) ?: throw Status.NOT_FOUND
                .withDescription("Stock not found: store=${request.storeId}, product=${request.productId}")
                .asRuntimeException()
        responseObserver.onNext(
            GetStockResponse.newBuilder().setStock(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listStocks(
        request: ListStocksRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListStocksResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20
        val (stocks, totalCount) =
            stockService.listStocks(
                storeId = request.storeId.toUUID(),
                lowStockOnly = request.lowStockOnly,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListStocksResponse
                .newBuilder()
                .addAllStocks(stocks.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun adjustStock(
        request: AdjustStockRequest,
        responseObserver: io.grpc.stub.StreamObserver<AdjustStockResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        val movementType = request.movementType.toDbValue()
        val entity =
            try {
                stockService.adjustStock(
                    storeId = request.storeId.toUUID(),
                    productId = request.productId.toUUID(),
                    quantityChange = request.quantityChange,
                    movementType = movementType,
                    referenceId = request.referenceId.ifBlank { null },
                    note = request.note.ifBlank { null },
                )
            } catch (e: IllegalArgumentException) {
                throw Status.FAILED_PRECONDITION
                    .withDescription(e.message)
                    .asRuntimeException()
            } catch (e: OptimisticLockException) {
                throw Status.ABORTED
                    .withDescription("Concurrent modification detected for stock: store=${request.storeId}, product=${request.productId}")
                    .asRuntimeException()
            }
        responseObserver.onNext(
            AdjustStockResponse.newBuilder().setStock(entity.toProto()).build(),
        )
        responseObserver.onCompleted()
    }

    override fun listStockMovements(
        request: ListStockMovementsRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListStockMovementsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20

        val startDate =
            if (request.hasDateRange() && request.dateRange.start.isNotBlank()) {
                Instant.parse(request.dateRange.start)
            } else {
                null
            }
        val endDate =
            if (request.hasDateRange() && request.dateRange.end.isNotBlank()) {
                Instant.parse(request.dateRange.end)
            } else {
                null
            }

        val (movements, totalCount) =
            stockService.listMovements(
                storeId = request.storeId.toUUID(),
                productId = request.productId.uuidOrNull(),
                startDate = startDate,
                endDate = endDate,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListStockMovementsResponse
                .newBuilder()
                .addAllMovements(movements.map { it.toProto() })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === PurchaseOrder ===

    override fun createPurchaseOrder(
        request: CreatePurchaseOrderRequest,
        responseObserver: io.grpc.stub.StreamObserver<CreatePurchaseOrderResponse>,
    ) {
        tenantHelper.setupTenantContextWithoutFilter()
        try {
            val items =
                request.itemsList.map { item ->
                    PurchaseOrderItemInput(
                        productId = item.productId.toUUID(),
                        orderedQuantity = item.orderedQuantity,
                        unitCost = item.unitCost,
                    )
                }
            val order =
                purchaseOrderService.create(
                    storeId = request.storeId.toUUID(),
                    supplierName = request.supplierName,
                    note = request.note.ifBlank { null },
                    items = items,
                )
            val orderItems = purchaseOrderService.getItems(order.id)
            responseObserver.onNext(
                CreatePurchaseOrderResponse
                    .newBuilder()
                    .setPurchaseOrder(order.toProto(orderItems))
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        }
    }

    override fun getPurchaseOrder(
        request: GetPurchaseOrderRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetPurchaseOrderResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val order =
            purchaseOrderService.findById(request.id.toUUID())
                ?: throw Status.NOT_FOUND.withDescription("PurchaseOrder not found: ${request.id}").asRuntimeException()
        val items = purchaseOrderService.getItems(order.id)
        responseObserver.onNext(
            GetPurchaseOrderResponse
                .newBuilder()
                .setPurchaseOrder(order.toProto(items))
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun listPurchaseOrders(
        request: ListPurchaseOrdersRequest,
        responseObserver: io.grpc.stub.StreamObserver<ListPurchaseOrdersResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20
        val statusFilter = request.status.toDbValueOrNull()
        val (orders, totalCount) =
            purchaseOrderService.list(
                storeId = request.storeId.toUUID(),
                status = statusFilter,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        responseObserver.onNext(
            ListPurchaseOrdersResponse
                .newBuilder()
                .addAllPurchaseOrders(orders.map { it.toProto(purchaseOrderService.getItems(it.id)) })
                .setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun updatePurchaseOrderStatus(
        request: UpdatePurchaseOrderStatusRequest,
        responseObserver: io.grpc.stub.StreamObserver<UpdatePurchaseOrderStatusResponse>,
    ) {
        tenantHelper.setupTenantContext()
        try {
            val newStatus =
                when (request.status) {
                    PurchaseOrderStatus.PURCHASE_ORDER_STATUS_ORDERED -> "ORDERED"
                    PurchaseOrderStatus.PURCHASE_ORDER_STATUS_RECEIVED -> "RECEIVED"
                    PurchaseOrderStatus.PURCHASE_ORDER_STATUS_CANCELLED -> "CANCELLED"
                    else -> throw Status.INVALID_ARGUMENT.withDescription("status is required").asRuntimeException()
                }
            val receivedItems =
                request.receivedItemsList.map { item ->
                    ReceivedItemInput(
                        productId = item.productId.toUUID(),
                        receivedQuantity = item.receivedQuantity,
                    )
                }
            val order =
                purchaseOrderService.updateStatus(
                    id = request.id.toUUID(),
                    newStatus = newStatus,
                    receivedItems = receivedItems,
                )
            val items = purchaseOrderService.getItems(order.id)
            responseObserver.onNext(
                UpdatePurchaseOrderStatusResponse
                    .newBuilder()
                    .setPurchaseOrder(order.toProto(items))
                    .build(),
            )
            responseObserver.onCompleted()
        } catch (e: IllegalArgumentException) {
            throw Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        }
    }

    // === Mapper Extensions ===

    private fun PurchaseOrderEntity.toProto(items: List<PurchaseOrderItemEntity>): PurchaseOrder =
        PurchaseOrder
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setStatus(status.toPurchaseOrderStatus())
            .addAllItems(items.map { it.toProto() })
            .setSupplierName(supplierName)
            .setNote(note.orEmpty())
            .setOrderedAt(orderedAt?.toString().orEmpty())
            .setReceivedAt(receivedAt?.toString().orEmpty())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun PurchaseOrderItemEntity.toProto(): PurchaseOrderItem =
        PurchaseOrderItem
            .newBuilder()
            .setId(id.toString())
            .setProductId(productId.toString())
            .setOrderedQuantity(orderedQuantity)
            .setReceivedQuantity(receivedQuantity)
            .setUnitCost(unitCost)
            .build()

    private fun String.toPurchaseOrderStatus(): PurchaseOrderStatus =
        when (this) {
            "DRAFT" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_DRAFT
            "ORDERED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_ORDERED
            "RECEIVED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_RECEIVED
            "CANCELLED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_CANCELLED
            else -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_UNSPECIFIED
        }

    private fun PurchaseOrderStatus.toDbValueOrNull(): String? =
        when (this) {
            PurchaseOrderStatus.PURCHASE_ORDER_STATUS_DRAFT -> "DRAFT"
            PurchaseOrderStatus.PURCHASE_ORDER_STATUS_ORDERED -> "ORDERED"
            PurchaseOrderStatus.PURCHASE_ORDER_STATUS_RECEIVED -> "RECEIVED"
            PurchaseOrderStatus.PURCHASE_ORDER_STATUS_CANCELLED -> "CANCELLED"
            else -> null
        }

    private fun StockEntity.toProto(): Stock =
        Stock
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setProductId(productId.toString())
            .setQuantity(quantity)
            .setLowStockThreshold(lowStockThreshold)
            .setUpdatedAt(updatedAt.toString())
            .build()

    private fun StockMovementEntity.toProto(): StockMovement =
        StockMovement
            .newBuilder()
            .setId(id.toString())
            .setOrganizationId(organizationId.toString())
            .setStoreId(storeId.toString())
            .setProductId(productId.toString())
            .setMovementType(movementType.toProtoMovementType())
            .setQuantity(quantity)
            .setReferenceId(referenceId.orEmpty())
            .setNote(note.orEmpty())
            .setCreatedAt(createdAt.toString())
            .build()

    // === Utility Extensions ===

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun String.uuidOrNull(): UUID? = if (isBlank()) null else toUUID()

    private fun MovementType.toDbValue(): String =
        when (this) {
            MovementType.MOVEMENT_TYPE_SALE -> {
                "SALE"
            }

            MovementType.MOVEMENT_TYPE_RETURN -> {
                "RETURN"
            }

            MovementType.MOVEMENT_TYPE_RECEIPT -> {
                "RECEIPT"
            }

            MovementType.MOVEMENT_TYPE_ADJUSTMENT -> {
                "ADJUSTMENT"
            }

            MovementType.MOVEMENT_TYPE_TRANSFER -> {
                "TRANSFER"
            }

            else -> {
                throw Status.INVALID_ARGUMENT
                    .withDescription("movement_type is required")
                    .asRuntimeException()
            }
        }

    private fun String.toProtoMovementType(): MovementType =
        when (this) {
            "SALE" -> MovementType.MOVEMENT_TYPE_SALE
            "RETURN" -> MovementType.MOVEMENT_TYPE_RETURN
            "RECEIPT" -> MovementType.MOVEMENT_TYPE_RECEIPT
            "ADJUSTMENT" -> MovementType.MOVEMENT_TYPE_ADJUSTMENT
            "TRANSFER" -> MovementType.MOVEMENT_TYPE_TRANSFER
            else -> MovementType.MOVEMENT_TYPE_UNSPECIFIED
        }
}
