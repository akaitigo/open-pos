package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
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
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * 在庫移動 REST リソース (#145)。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * proto 定義が追加され次第、gRPC 呼び出しに置き換える。
 */
@Path("/api/stock-transfers")
@Blocking
@Tag(name = "Stock Transfers", description = "在庫移動管理API")
class StockTransferResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplementedResponse(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Stock transfer API is not yet implemented"))
            .build()

    @GET
    @Operation(summary = "在庫移動一覧を取得する")
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
    }

    @POST
    @Operation(summary = "在庫移動を作成する")
    fun create(body: CreateStockTransferBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "在庫移動を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
    }

    @PUT
    @Path("/{id}/status")
    @Operation(summary = "在庫移動のステータスを更新する")
    fun updateStatus(
        @PathParam("id") id: String,
        body: UpdateStockTransferStatusBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
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

data class UpdateStockTransferStatusBody(
    val status: String,
    val note: String? = null,
)
