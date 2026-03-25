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
 * 予約注文 REST リソース（#193）。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/reservations")
@Blocking
@DenyAll
class ReservationResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Reservation API is not yet implemented"))
            .build()

    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Response = notImplemented()

    @POST
    fun create(body: CreateReservationBody): Response = notImplemented()

    @PUT
    @Path("/{id}/fulfill")
    fun fulfill(
        @PathParam("id") id: String,
    ): Response = notImplemented()

    @PUT
    @Path("/{id}/cancel")
    fun cancel(
        @PathParam("id") id: String,
    ): Response = notImplemented()
}

data class CreateReservationBody(
    val storeId: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val items: List<ReservationItemBody> = emptyList(),
    val reservedUntil: String,
    val note: String? = null,
)

data class ReservationItemBody(
    val productId: String,
    val quantity: Int,
)
