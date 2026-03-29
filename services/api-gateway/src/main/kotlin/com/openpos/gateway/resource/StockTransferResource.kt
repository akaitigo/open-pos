package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
import com.openpos.gateway.config.requireValidPage
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
import openpos.common.v1.PaginationRequest
import openpos.inventory.v1.CompleteStockTransferRequest
import openpos.inventory.v1.CreateStockTransferRequest
import openpos.inventory.v1.GetStockTransferRequest
import openpos.inventory.v1.InventoryServiceGrpc
import openpos.inventory.v1.ListStockTransfersRequest
import openpos.inventory.v1.StockTransferItemInput
import openpos.inventory.v1.StockTransferStatus
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * 在庫移動 REST リソース (#145)。
 * 店舗間の在庫移動を管理するエンドポイント。
 */
@Path("/api/stock-transfers")
@Blocking
@Tag(name = "Stock Transfers", description = "在庫移動管理API")
class StockTransferResource {
    @Inject
    @GrpcClient("inventory-service")
    lateinit var stub: InventoryServiceGrpc.InventoryServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "在庫移動一覧を取得する")
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        requireValidPage(page)
        val request =
            ListStockTransfersRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    storeId?.let { setStoreId(it) }
                    status?.let { setStatus(parseStockTransferStatus(it)) }
                }.build()
        val response = grpc.withTenant(stub).listStockTransfers(request)
        return paginatedResponse(
            data = response.stockTransfersList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @POST
    @Operation(summary = "在庫移動を作成する")
    fun create(body: CreateStockTransferBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateStockTransferRequest
                .newBuilder()
                .setFromStoreId(body.fromStoreId)
                .setToStoreId(body.toStoreId)
                .addAllItems(
                    body.items.map { item ->
                        StockTransferItemInput
                            .newBuilder()
                            .setProductId(item.productId)
                            .setQuantity(item.quantity)
                            .build()
                    },
                ).apply {
                    body.note?.let { setNote(it) }
                }.build()
        val response = grpc.withTenant(stub).createStockTransfer(request)
        return Response.status(Response.Status.CREATED).entity(response.stockTransfer.toMap()).build()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "在庫移動を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        return grpc
            .withTenant(stub)
            .getStockTransfer(GetStockTransferRequest.newBuilder().setId(id).build())
            .stockTransfer
            .toMap()
    }

    @PUT
    @Path("/{id}/complete")
    @Operation(summary = "在庫移動を完了する（入荷確認・在庫調整）")
    fun complete(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        return grpc
            .withTenant(stub)
            .completeStockTransfer(CompleteStockTransferRequest.newBuilder().setId(id).build())
            .stockTransfer
            .toMap()
    }
}

data class CreateStockTransferBody(
    val fromStoreId: String,
    val toStoreId: String,
    val items: List<StockTransferItemBody>,
    val note: String? = null,
)

data class StockTransferItemBody(
    val productId: String,
    val quantity: Int,
)

private fun parseStockTransferStatus(value: String): StockTransferStatus =
    when (value.uppercase()) {
        "PENDING" -> StockTransferStatus.STOCK_TRANSFER_STATUS_PENDING
        "IN_TRANSIT" -> StockTransferStatus.STOCK_TRANSFER_STATUS_IN_TRANSIT
        "COMPLETED" -> StockTransferStatus.STOCK_TRANSFER_STATUS_COMPLETED
        "CANCELLED" -> StockTransferStatus.STOCK_TRANSFER_STATUS_CANCELLED
        else -> StockTransferStatus.STOCK_TRANSFER_STATUS_UNSPECIFIED
    }

private fun openpos.inventory.v1.StockTransfer.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id,
        "organizationId" to organizationId,
        "fromStoreId" to fromStoreId,
        "toStoreId" to toStoreId,
        "items" to
            itemsList.map { item ->
                mapOf(
                    "productId" to item.productId,
                    "quantity" to item.quantity,
                )
            },
        "status" to
            when (status) {
                StockTransferStatus.STOCK_TRANSFER_STATUS_PENDING -> "PENDING"
                StockTransferStatus.STOCK_TRANSFER_STATUS_IN_TRANSIT -> "IN_TRANSIT"
                StockTransferStatus.STOCK_TRANSFER_STATUS_COMPLETED -> "COMPLETED"
                StockTransferStatus.STOCK_TRANSFER_STATUS_CANCELLED -> "CANCELLED"
                else -> "UNSPECIFIED"
            },
        "note" to note.ifEmpty { null },
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
    )
