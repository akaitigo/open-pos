package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * 値引き理由コード REST リソース（#216）。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/discount-reasons")
@Blocking
class DiscountReasonResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Discount reason API is not yet implemented"))
            .build()

    @GET
    fun list(): Response = notImplemented()

    @POST
    fun create(body: CreateDiscountReasonBody): Response = notImplemented()

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateDiscountReasonBody,
    ): Response = notImplemented()
}

data class CreateDiscountReasonBody(
    val code: String,
    val description: String,
)

data class UpdateDiscountReasonBody(
    val description: String? = null,
    val isActive: Boolean? = null,
)
