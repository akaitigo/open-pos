package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * お気に入り商品 REST リソース（#188）。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/staff/{staffId}/favorites")
@Blocking
class FavoriteProductResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Favorite product API is not yet implemented"))
            .build()

    @GET
    fun list(
        @PathParam("staffId") staffId: String,
    ): Response = notImplemented()

    @POST
    fun toggle(
        @PathParam("staffId") staffId: String,
        body: ToggleFavoriteBody,
    ): Response = notImplemented()

    @DELETE
    @Path("/{productId}")
    fun remove(
        @PathParam("staffId") staffId: String,
        @PathParam("productId") productId: String,
    ): Response = notImplemented()
}

data class ToggleFavoriteBody(
    val productId: String,
)
