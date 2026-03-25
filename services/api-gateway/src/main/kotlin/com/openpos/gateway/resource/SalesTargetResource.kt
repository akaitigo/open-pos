package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 売上目標管理 REST リソース（#214）。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/sales-targets")
@Blocking
class SalesTargetResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Sales target API is not yet implemented"))
            .build()

    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("month") month: String?,
    ): Response = notImplemented()

    @PUT
    fun upsert(body: UpsertSalesTargetBody): Response = notImplemented()
}

data class UpsertSalesTargetBody(
    val storeId: String? = null,
    val targetMonth: String,
    val targetAmount: Long,
)
