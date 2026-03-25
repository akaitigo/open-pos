package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.annotation.security.DenyAll
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 在庫移動 REST リソース (#145)。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/stock-transfers")
@Blocking
@DenyAll
class StockTransferResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Stock transfer API is not yet implemented"))
            .build()

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Response = notImplemented()

    @POST
    fun create(body: Map<String, Any?>): Response = notImplemented()

    @PUT
    @Path("/{id}/status")
    fun updateStatus(
        @PathParam("id") id: String,
        body: Map<String, Any?>,
    ): Response = notImplemented()
}
