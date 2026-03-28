package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.store.v1.AddStampRequest
import openpos.store.v1.GetStampCardRequest
import openpos.store.v1.IssueStampCardRequest
import openpos.store.v1.RedeemStampRewardRequest
import openpos.store.v1.StoreServiceGrpc
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * デジタルスタンプカード REST リソース（#222）。
 * store-service の StampCard gRPC RPCs にプロキシする。
 */
@Path("/api/stamp-cards")
@Blocking
@Tag(name = "StampCards", description = "スタンプカード管理API")
class StampCardResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Path("/{customerId}")
    @Operation(summary = "顧客のスタンプカードを取得する")
    fun get(
        @PathParam("customerId") customerId: String,
    ): Map<String, Any?> {
        val request = GetStampCardRequest.newBuilder().setCustomerId(customerId).build()
        return grpc
            .withTenant(stub)
            .getStampCard(request)
            .stampCard
            .toMap()
    }

    @POST
    @Operation(summary = "スタンプカードを発行する")
    fun issue(body: IssueStampCardBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        val request =
            IssueStampCardRequest
                .newBuilder()
                .setCustomerId(body.customerId)
                .setMaxStamps(body.maxStamps)
                .apply { body.rewardDescription?.let { setRewardDescription(it) } }
                .build()
        val response = grpc.withTenant(stub).issueStampCard(request)
        return Response.status(Response.Status.CREATED).entity(response.stampCard.toMap()).build()
    }

    @POST
    @Path("/{customerId}/stamp")
    @Operation(summary = "スタンプを追加する")
    fun addStamp(
        @PathParam("customerId") customerId: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        val request = AddStampRequest.newBuilder().setCustomerId(customerId).build()
        return grpc
            .withTenant(stub)
            .addStamp(request)
            .stampCard
            .toMap()
    }

    @POST
    @Path("/{customerId}/redeem")
    @Operation(summary = "スタンプ報酬を交換する")
    fun redeemReward(
        @PathParam("customerId") customerId: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER", "STAFF")
        val request = RedeemStampRewardRequest.newBuilder().setCustomerId(customerId).build()
        return grpc
            .withTenant(stub)
            .redeemStampReward(request)
            .stampCard
            .toMap()
    }
}

data class IssueStampCardBody(
    val customerId: String,
    val maxStamps: Int = 10,
    val rewardDescription: String? = null,
)
