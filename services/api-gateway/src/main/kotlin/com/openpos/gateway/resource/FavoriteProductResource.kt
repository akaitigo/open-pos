package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * お気に入り商品 REST リソース（#188）。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * proto 定義が追加され次第、gRPC 呼び出しに置き換える。
 */
@Path("/api/staff/{staffId}/favorites")
@Blocking
@Tag(name = "Favorite Products", description = "お気に入り商品管理API")
class FavoriteProductResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplementedResponse(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Favorite product API is not yet implemented"))
            .build()

    @GET
    @Operation(summary = "スタッフのお気に入り商品一覧を取得する")
    fun list(
        @PathParam("staffId") staffId: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        return notImplementedResponse()
    }

    @POST
    @Operation(summary = "お気に入り商品を追加する")
    fun add(
        @PathParam("staffId") staffId: String,
        body: AddFavoriteBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        return notImplementedResponse()
    }

    @DELETE
    @Path("/{productId}")
    @Operation(summary = "お気に入り商品を削除する")
    fun remove(
        @PathParam("staffId") staffId: String,
        @PathParam("productId") productId: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        return notImplementedResponse()
    }
}

data class AddFavoriteBody(
    val productId: String,
)
