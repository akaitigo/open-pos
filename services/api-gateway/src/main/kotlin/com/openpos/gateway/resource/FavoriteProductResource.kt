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
 * プレースホルダー実装。
 */
@Path("/api/staff/{staffId}/favorites")
@Blocking
class FavoriteProductResource {
    @GET
    fun list(
        @PathParam("staffId") staffId: String,
    ): List<Map<String, Any>> {
        // プレースホルダー
        return emptyList()
    }

    @POST
    fun toggle(
        @PathParam("staffId") staffId: String,
        body: ToggleFavoriteBody,
    ): Response =
        Response
            .ok(
                mapOf(
                    "staffId" to staffId,
                    "productId" to body.productId,
                    "isFavorite" to true,
                ),
            ).build()

    @DELETE
    @Path("/{productId}")
    fun remove(
        @PathParam("staffId") staffId: String,
        @PathParam("productId") productId: String,
    ): Response = Response.noContent().build()
}

data class ToggleFavoriteBody(
    val productId: String,
)
