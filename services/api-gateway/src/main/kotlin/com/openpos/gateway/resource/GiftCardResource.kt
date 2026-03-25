package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.annotation.security.DenyAll
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * ギフトカード REST リソース (#142)。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 Not Implemented を返す。
 * 本番では実装されるまで明示的に 501 でブロックする。
 */
@Path("/api/gift-cards")
@Blocking
@DenyAll
class GiftCardResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Gift card API is not yet implemented"))
            .build()

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Response = notImplemented()

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Response = notImplemented()

    @POST
    fun create(body: Map<String, Any?>): Response = notImplemented()

    @POST
    @Path("/{code}/activate")
    fun activate(
        @PathParam("code") code: String,
    ): Response = notImplemented()

    @POST
    @Path("/{code}/redeem")
    fun redeem(
        @PathParam("code") code: String,
        body: Map<String, Any?>,
    ): Response = notImplemented()
}
