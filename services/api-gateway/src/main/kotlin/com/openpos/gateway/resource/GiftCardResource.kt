package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.pos.v1.ActivateGiftCardRequest
import openpos.pos.v1.CreateGiftCardRequest
import openpos.pos.v1.GetGiftCardBalanceRequest
import openpos.pos.v1.GetGiftCardRequest
import openpos.pos.v1.ListGiftCardsRequest
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.RedeemGiftCardRequest
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * ギフトカード REST リソース (#142)。
 * pos-service の GiftCard gRPC RPCs にプロキシする。
 */
@Path("/api/gift-cards")
@Blocking
@Tag(name = "GiftCards", description = "ギフトカード管理API")
class GiftCardResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "ギフトカード一覧を取得する")
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        val request = ListGiftCardsRequest.getDefaultInstance()
        val response = grpc.withTenant(stub).listGiftCards(request)
        return mapOf(
            "data" to response.giftCardsList.map { it.toMap() },
        )
    }

    @GET
    @Path("/{code}")
    @Operation(summary = "コードでギフトカードを取得する")
    fun get(
        @PathParam("code") code: String,
    ): Map<String, Any?> {
        val request = GetGiftCardRequest.newBuilder().setCode(code).build()
        return grpc
            .withTenant(stub)
            .getGiftCard(request)
            .giftCard
            .toMap()
    }

    @POST
    @Operation(summary = "ギフトカードを発行する")
    fun create(body: CreateGiftCardBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateGiftCardRequest
                .newBuilder()
                .setInitialAmount(body.initialAmount)
                .apply { body.expiresAt?.let { setExpiresAt(it) } }
                .build()
        val response = grpc.withTenant(stub).createGiftCard(request)
        return Response.status(Response.Status.CREATED).entity(response.giftCard.toMap()).build()
    }

    @POST
    @Path("/{code}/activate")
    @Operation(summary = "ギフトカードを有効化する")
    fun activate(
        @PathParam("code") code: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        val request = ActivateGiftCardRequest.newBuilder().setCode(code).build()
        return grpc
            .withTenant(stub)
            .activateGiftCard(request)
            .giftCard
            .toMap()
    }

    @POST
    @Path("/{code}/redeem")
    @Operation(summary = "ギフトカードから残高を利用する")
    fun redeem(
        @PathParam("code") code: String,
        body: RedeemGiftCardBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        require(body.amount > 0) { "Redeem amount must be positive" }
        val request =
            RedeemGiftCardRequest
                .newBuilder()
                .setCode(code)
                .setAmount(body.amount)
                .build()
        return grpc
            .withTenant(stub)
            .redeemGiftCard(request)
            .giftCard
            .toMap()
    }

    @GET
    @Path("/{code}/balance")
    @Operation(summary = "ギフトカードの残高を確認する")
    fun checkBalance(
        @PathParam("code") code: String,
    ): Map<String, Any> {
        val request = GetGiftCardBalanceRequest.newBuilder().setCode(code).build()
        val response = grpc.withTenant(stub).getGiftCardBalance(request)
        return mapOf(
            "code" to response.code,
            "balance" to response.balance,
            "status" to response.status,
        )
    }
}

data class CreateGiftCardBody(
    val initialAmount: Long,
    val expiresAt: String? = null,
)

data class RedeemGiftCardBody(
    val amount: Long,
)
