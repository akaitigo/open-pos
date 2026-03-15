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
 * プレースホルダー実装。
 */
@Path("/api/discount-reasons")
@Blocking
class DiscountReasonResource {
    @GET
    fun list(): List<Map<String, Any>> {
        // プレースホルダー
        return emptyList()
    }

    @POST
    fun create(body: CreateDiscountReasonBody): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                mapOf(
                    "code" to body.code,
                    "description" to body.description,
                    "isActive" to true,
                ),
            ).build()

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateDiscountReasonBody,
    ): Map<String, Any?> =
        mapOf(
            "id" to id,
            "description" to body.description,
            "isActive" to body.isActive,
        )
}

data class CreateDiscountReasonBody(
    val code: String,
    val description: String,
)

data class UpdateDiscountReasonBody(
    val description: String? = null,
    val isActive: Boolean? = null,
)
