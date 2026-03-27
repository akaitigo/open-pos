package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import java.time.Instant
import java.util.UUID

/**
 * ギフトカード REST リソース (#142)。
 * gRPC バックエンド未整備のため、ゲートウェイレベルで一時的に管理する。
 * バックエンドサービス実装後に gRPC 呼び出しに切り替える。
 */
@Path("/api/gift-cards")
@Blocking
@Tag(name = "GiftCards", description = "ギフトカード管理API")
class GiftCardResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "ギフトカード一覧を取得する")
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        // TODO: gRPC バックエンド実装後に ListGiftCards RPC に切り替え
        return mapOf(
            "data" to emptyList<Any>(),
            "pagination" to
                mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "totalCount" to 0,
                    "totalPages" to 0,
                ),
        )
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "IDでギフトカードを取得する")
    fun get(
        @PathParam("id") id: String,
    ): Response {
        // TODO: gRPC バックエンド実装後に GetGiftCard RPC に切り替え
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to "NOT_FOUND", "message" to "Gift card not found"))
            .build()
    }

    @POST
    @Operation(summary = "ギフトカードを発行する")
    fun create(body: CreateGiftCardBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val now = Instant.now().toString()
        val card =
            mapOf(
                "id" to UUID.randomUUID().toString(),
                "code" to generateGiftCardCode(),
                "initialAmount" to body.initialAmount,
                "balance" to body.initialAmount,
                "status" to "ACTIVE",
                "issuedAt" to now,
                "expiresAt" to body.expiresAt,
                "createdAt" to now,
                "updatedAt" to now,
            )
        return Response.status(Response.Status.CREATED).entity(card).build()
    }

    @POST
    @Path("/{code}/activate")
    @Operation(summary = "ギフトカードを有効化する")
    fun activate(
        @PathParam("code") code: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        // TODO: gRPC バックエンド実装後に ActivateGiftCard RPC に切り替え
        return Response
            .ok(
                mapOf(
                    "code" to code,
                    "status" to "ACTIVE",
                    "activatedAt" to Instant.now().toString(),
                ),
            ).build()
    }

    @POST
    @Path("/{code}/redeem")
    @Operation(summary = "ギフトカードから残高を利用する")
    fun redeem(
        @PathParam("code") code: String,
        body: RedeemGiftCardBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        require(body.amount > 0) { "Redeem amount must be positive" }
        // TODO: gRPC バックエンド実装後に RedeemGiftCard RPC に切り替え
        return Response
            .ok(
                mapOf(
                    "code" to code,
                    "redeemedAmount" to body.amount,
                    "remainingBalance" to 0L,
                    "redeemedAt" to Instant.now().toString(),
                ),
            ).build()
    }

    @GET
    @Path("/{code}/balance")
    @Operation(summary = "ギフトカードの残高を確認する")
    fun checkBalance(
        @PathParam("code") code: String,
    ): Response {
        // TODO: gRPC バックエンド実装後に GetGiftCardBalance RPC に切り替え
        return Response
            .status(Response.Status.NOT_FOUND)
            .entity(mapOf("error" to "NOT_FOUND", "message" to "Gift card not found"))
            .build()
    }

    private fun generateGiftCardCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..16).map { chars.random() }.chunked(4).joinToString("-") { it.joinToString("") }
    }
}

data class CreateGiftCardBody(
    val initialAmount: Long,
    val expiresAt: String? = null,
)

data class RedeemGiftCardBody(
    val amount: Long,
)
