package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
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
 * プレースホルダー実装。
 */
@Path("/api/reservations")
@Blocking
class ReservationResource {
    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> =
        mapOf(
            "data" to emptyList<Any>(),
            "pagination" to
                mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "totalCount" to 0,
                    "totalPages" to 0,
                ),
        )

    @POST
    fun create(body: CreateReservationBody): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                mapOf(
                    "storeId" to body.storeId,
                    "customerName" to (body.customerName ?: ""),
                    "status" to "RESERVED",
                    "message" to "予約を作成しました",
                ),
            ).build()

    @PUT
    @Path("/{id}/fulfill")
    fun fulfill(
        @PathParam("id") id: String,
    ): Map<String, Any> =
        mapOf(
            "id" to id,
            "status" to "FULFILLED",
        )

    @PUT
    @Path("/{id}/cancel")
    fun cancel(
        @PathParam("id") id: String,
    ): Map<String, Any> =
        mapOf(
            "id" to id,
            "status" to "CANCELLED",
        )
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
