package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest
import openpos.inventory.v1.AdjustStockRequest
import openpos.inventory.v1.CreatePurchaseOrderRequest
import openpos.inventory.v1.GetPurchaseOrderRequest
import openpos.inventory.v1.GetStockRequest
import openpos.inventory.v1.InventoryServiceGrpc
import openpos.inventory.v1.ListPurchaseOrdersRequest
import openpos.inventory.v1.ListStockMovementsRequest
import openpos.inventory.v1.ListStocksRequest
import openpos.inventory.v1.MovementType
import openpos.inventory.v1.PurchaseOrderItemInput
import openpos.inventory.v1.PurchaseOrderStatus
import openpos.inventory.v1.ReceivedItemInput
import openpos.inventory.v1.UpdatePurchaseOrderStatusRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/inventory")
@Blocking
@Timeout(30000)
class InventoryResource {
    @Inject
    @GrpcClient("inventory-service")
    lateinit var stub: InventoryServiceGrpc.InventoryServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    // === Stocks ===

    @GET
    @Path("/stocks")
    fun listStocks(
        @QueryParam("storeId") storeId: String,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("lowStockOnly") @DefaultValue("false") lowStockOnly: Boolean,
    ): Map<String, Any> {
        val request =
            ListStocksRequest
                .newBuilder()
                .setStoreId(storeId)
                .setLowStockOnly(lowStockOnly)
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).listStocks(request)
        return paginatedResponse(
            data = response.stocksList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @GET
    @Path("/stocks/{storeId}/{productId}")
    fun getStock(
        @PathParam("storeId") storeId: String,
        @PathParam("productId") productId: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getStock(
                GetStockRequest
                    .newBuilder()
                    .setStoreId(storeId)
                    .setProductId(productId)
                    .build(),
            ).stock
            .toMap()

    @POST
    @Path("/stocks/adjust")
    fun adjustStock(body: AdjustStockBody): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            AdjustStockRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setProductId(body.productId)
                .setQuantityChange(body.quantityChange)
                .setMovementType(parseMovementType(body.movementType))
                .apply {
                    body.referenceId?.let { setReferenceId(it) }
                    body.note?.let { setNote(it) }
                }.build()
        return grpc
            .withTenant(stub)
            .adjustStock(request)
            .stock
            .toMap()
    }

    // === Movements ===

    @GET
    @Path("/movements")
    fun listMovements(
        @QueryParam("storeId") storeId: String,
        @QueryParam("productId") productId: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
    ): Map<String, Any> {
        val request =
            ListStockMovementsRequest
                .newBuilder()
                .setStoreId(storeId)
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    productId?.let { setProductId(it) }
                    if (startDate != null || endDate != null) {
                        setDateRange(
                            DateRange
                                .newBuilder()
                                .apply {
                                    startDate?.let { setStart(it) }
                                    endDate?.let { setEnd(it) }
                                }.build(),
                        )
                    }
                }.build()
        val response = grpc.withTenant(stub).listStockMovements(request)
        return paginatedResponse(
            data = response.movementsList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    // === Purchase Orders ===

    @POST
    @Path("/purchase-orders")
    fun createPurchaseOrder(body: CreatePurchaseOrderBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreatePurchaseOrderRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setSupplierName(body.supplierName)
                .apply { body.note?.let { setNote(it) } }
                .addAllItems(
                    body.items.map { item ->
                        PurchaseOrderItemInput
                            .newBuilder()
                            .setProductId(item.productId)
                            .setOrderedQuantity(item.orderedQuantity)
                            .setUnitCost(item.unitCost)
                            .build()
                    },
                ).build()
        val response = grpc.withTenant(stub).createPurchaseOrder(request)
        return Response.status(Response.Status.CREATED).entity(response.purchaseOrder.toMap()).build()
    }

    @GET
    @Path("/purchase-orders/{id}")
    fun getPurchaseOrder(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getPurchaseOrder(GetPurchaseOrderRequest.newBuilder().setId(id).build())
            .purchaseOrder
            .toMap()

    @GET
    @Path("/purchase-orders")
    fun listPurchaseOrders(
        @QueryParam("storeId") storeId: String,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        val request =
            ListPurchaseOrdersRequest
                .newBuilder()
                .setStoreId(storeId)
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    status?.let { setStatus(parsePurchaseOrderStatus(it)) }
                }.build()
        val response = grpc.withTenant(stub).listPurchaseOrders(request)
        return paginatedResponse(
            data = response.purchaseOrdersList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @PUT
    @Path("/purchase-orders/{id}/status")
    fun updatePurchaseOrderStatus(
        @PathParam("id") id: String,
        body: UpdatePurchaseOrderStatusBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            UpdatePurchaseOrderStatusRequest
                .newBuilder()
                .setId(id)
                .setStatus(parsePurchaseOrderStatus(body.status))
                .addAllReceivedItems(
                    (body.receivedItems ?: emptyList()).map { item ->
                        ReceivedItemInput
                            .newBuilder()
                            .setProductId(item.productId)
                            .setReceivedQuantity(item.receivedQuantity)
                            .build()
                    },
                ).build()
        return grpc
            .withTenant(stub)
            .updatePurchaseOrderStatus(request)
            .purchaseOrder
            .toMap()
    }
}

private fun parseMovementType(value: String): MovementType =
    when (value.uppercase()) {
        "SALE" -> MovementType.MOVEMENT_TYPE_SALE
        "RETURN" -> MovementType.MOVEMENT_TYPE_RETURN
        "RECEIPT" -> MovementType.MOVEMENT_TYPE_RECEIPT
        "ADJUSTMENT" -> MovementType.MOVEMENT_TYPE_ADJUSTMENT
        "TRANSFER" -> MovementType.MOVEMENT_TYPE_TRANSFER
        else -> MovementType.MOVEMENT_TYPE_UNSPECIFIED
    }

private fun parsePurchaseOrderStatus(value: String): PurchaseOrderStatus =
    when (value.uppercase()) {
        "DRAFT" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_DRAFT
        "ORDERED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_ORDERED
        "RECEIVED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_RECEIVED
        "CANCELLED" -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_CANCELLED
        else -> PurchaseOrderStatus.PURCHASE_ORDER_STATUS_UNSPECIFIED
    }

data class AdjustStockBody(
    val storeId: String,
    val productId: String,
    val quantityChange: Int,
    val movementType: String,
    val referenceId: String? = null,
    val note: String? = null,
)

data class CreatePurchaseOrderBody(
    val storeId: String,
    val supplierName: String,
    val note: String? = null,
    val items: List<PurchaseOrderItemBody>,
)

data class PurchaseOrderItemBody(
    val productId: String,
    val orderedQuantity: Int,
    val unitCost: Long,
)

data class UpdatePurchaseOrderStatusBody(
    val status: String,
    val receivedItems: List<ReceivedItemBody>? = null,
)

data class ReceivedItemBody(
    val productId: String,
    val receivedQuantity: Int,
)
