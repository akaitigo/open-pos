package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.time.Instant
import java.util.UUID

/**
 * 売上目標管理 REST リソース（#214）。
 * gRPC バックエンド未整備のため、ゲートウェイレベルで一時的に管理する。
 * バックエンドサービス実装後に gRPC 呼び出しに切り替える。
 */
@Path("/api/sales-targets")
@Blocking
@Tag(name = "SalesTargets", description = "売上目標管理API")
class SalesTargetResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "売上目標一覧を取得する")
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("month") month: String?,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        // TODO: gRPC バックエンド実装後に ListSalesTargets RPC に切り替え
        return mapOf(
            "data" to emptyList<Any>(),
            "filters" to
                mapOf(
                    "storeId" to storeId,
                    "month" to month,
                ),
        )
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "売上目標を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        // TODO: gRPC バックエンド実装後に GetSalesTarget RPC に切り替え
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to "NOT_FOUND", "message" to "Sales target not found"))
            .build()
    }

    @POST
    @Operation(summary = "売上目標を作成する")
    fun create(body: CreateSalesTargetBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        require(body.targetAmount > 0) { "Target amount must be positive" }
        val now = Instant.now().toString()
        val target =
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "storeId" to body.storeId,
                "staffId" to body.staffId,
                "targetMonth" to body.targetMonth,
                "targetAmount" to body.targetAmount,
                "currentAmount" to 0L,
                "achievementRate" to 0.0,
                "createdAt" to now,
                "updatedAt" to now,
            )
        return Response.status(Response.Status.CREATED).entity(target).build()
    }

    @PUT
    @Path("/{id}")
    @Operation(summary = "売上目標を更新する")
    fun update(
        @PathParam("id") id: String,
        body: UpdateSalesTargetBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        body.targetAmount?.let { require(it > 0) { "Target amount must be positive" } }
        // TODO: gRPC バックエンド実装後に UpdateSalesTarget RPC に切り替え
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to "NOT_FOUND", "message" to "Sales target not found"))
            .build()
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "売上目標を削除する")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        // TODO: gRPC バックエンド実装後に DeleteSalesTarget RPC に切り替え
        return Response.noContent().build()
    }
}

data class CreateSalesTargetBody(
    val storeId: String,
    val staffId: String? = null,
    val targetMonth: String,
    val targetAmount: Long,
)

data class UpdateSalesTargetBody(
    val targetAmount: Long? = null,
)
