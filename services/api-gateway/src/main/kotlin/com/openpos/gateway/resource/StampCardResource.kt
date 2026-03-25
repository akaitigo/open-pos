package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.annotation.security.DenyAll
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * デジタルスタンプカード REST リソース（#222）。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/stamp-cards")
@Blocking
@DenyAll
class StampCardResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Stamp card API is not yet implemented"))
            .build()

    @GET
    @Path("/{customerId}")
    fun get(
        @PathParam("customerId") customerId: String,
    ): Response = notImplemented()

    @POST
    @Path("/{customerId}/stamp")
    fun addStamp(
        @PathParam("customerId") customerId: String,
    ): Response = notImplemented()

    @POST
    @Path("/{customerId}/redeem")
    fun redeemReward(
        @PathParam("customerId") customerId: String,
    ): Response = notImplemented()
}
