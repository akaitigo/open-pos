package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * 値引き理由コード REST リソース（#216）。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * proto 定義が追加され次第、gRPC 呼び出しに置き換える。
 */
@Path("/api/discount-reasons")
@Blocking
@Tag(name = "Discount Reasons", description = "値引き理由コード管理API")
class DiscountReasonResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplementedResponse(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Discount reason API is not yet implemented"))
            .build()

    @GET
    @Operation(summary = "値引き理由コード一覧を取得する")
    fun list(): Response = notImplementedResponse()

    @POST
    @Operation(summary = "値引き理由コードを作成する")
    fun create(body: CreateDiscountReasonBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "値引き理由コードを更新する")
    fun update(
        @PathParam("id") id: String,
        body: UpdateDiscountReasonBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplementedResponse()
    }
}

data class CreateDiscountReasonBody(
    val code: String,
    val description: String,
)

data class UpdateDiscountReasonBody(
    val description: String? = null,
    val isActive: Boolean? = null,
)
