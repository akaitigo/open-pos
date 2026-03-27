package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.time.Instant
import java.util.UUID

/**
 * デジタルスタンプカード REST リソース（#222）。
 * gRPC バックエンド未整備のため、ゲートウェイレベルで一時的に管理する。
 * バックエンドサービス実装後に gRPC 呼び出しに切り替える。
 */
@Path("/api/stamp-cards")
@Blocking
@Tag(name = "StampCards", description = "スタンプカード管理API")
class StampCardResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Path("/{customerId}")
    @Operation(summary = "顧客のスタンプカードを取得する")
    fun get(
        @PathParam("customerId") customerId: String,
    ): Response {
        // TODO: gRPC バックエンド実装後に GetStampCard RPC に切り替え
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to "NOT_FOUND", "message" to "Stamp card not found for customer"))
            .build()
    }

    @POST
    @Operation(summary = "スタンプカードを発行する")
    fun issue(body: IssueStampCardBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        val now = Instant.now().toString()
        val card =
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "customerId" to body.customerId,
                "stampCount" to 0,
                "maxStamps" to body.maxStamps,
                "rewardDescription" to body.rewardDescription,
                "status" to "ACTIVE",
                "issuedAt" to now,
                "createdAt" to now,
                "updatedAt" to now,
            )
        return Response.status(Response.Status.CREATED).entity(card).build()
    }

    @POST
    @Path("/{customerId}/stamp")
    @Operation(summary = "スタンプを追加する")
    fun addStamp(
        @PathParam("customerId") customerId: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        // TODO: gRPC バックエンド実装後に AddStamp RPC に切り替え
        val now = Instant.now().toString()
        return Response
            .ok(
                mapOf(
                    "customerId" to customerId,
                    "stampCount" to 1,
                    "stampedAt" to now,
                ),
            ).build()
    }

    @POST
    @Path("/{customerId}/redeem")
    @Operation(summary = "スタンプ報酬を交換する")
    fun redeemReward(
        @PathParam("customerId") customerId: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        // TODO: gRPC バックエンド実装後に RedeemStampReward RPC に切り替え
        val now = Instant.now().toString()
        return Response
            .ok(
                mapOf(
                    "customerId" to customerId,
                    "redeemed" to true,
                    "stampCount" to 0,
                    "redeemedAt" to now,
                ),
            ).build()
    }
}

data class IssueStampCardBody(
    val customerId: String,
    val maxStamps: Int = 10,
    val rewardDescription: String? = null,
)
