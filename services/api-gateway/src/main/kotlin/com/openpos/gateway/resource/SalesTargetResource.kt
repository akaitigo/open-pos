package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.DeleteSalesTargetRequest
import openpos.analytics.v1.GetSalesTargetRequest
import openpos.analytics.v1.ListSalesTargetsRequest
import openpos.analytics.v1.UpsertSalesTargetRequest
import org.eclipse.microprofile.faulttolerance.Timeout
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.tags.Tag

/**
 * 売上目標管理 REST リソース。
 * analytics-service の gRPC バックエンドに委譲する。
 */
@Path("/api/sales-targets")
@Blocking
@Timeout(30000)
@Tag(name = "SalesTargets", description = "売上目標管理API")
class SalesTargetResource {
    @Inject
    @GrpcClient("analytics-service")
    lateinit var stub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Operation(summary = "売上目標一覧を取得する")
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("month") month: String?,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            ListSalesTargetsRequest
                .newBuilder()
                .apply {
                    storeId?.let { setStoreId(it) }
                    month?.let { setMonth(it) }
                }.build()
        val response = grpc.withTenant(stub).listSalesTargets(request)
        return mapOf("data" to response.salesTargetsList.map { it.toMap() })
    }

    @GET
    @Path("/{id}")
    @Operation(summary = "売上目標を取得する")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request = GetSalesTargetRequest.newBuilder().setId(id).build()
        return grpc
            .withTenant(stub)
            .getSalesTarget(request)
            .salesTarget
            .toMap()
    }

    @POST
    @Operation(summary = "売上目標を作成または更新する")
    fun upsert(body: UpsertSalesTargetBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        require(body.targetAmount > 0) { "Target amount must be positive" }
        val request =
            UpsertSalesTargetRequest
                .newBuilder()
                .setStoreId(body.storeId.orEmpty())
                .setTargetMonth(body.targetMonth)
                .setTargetAmount(body.targetAmount)
                .build()
        val response = grpc.withTenant(stub).upsertSalesTarget(request)
        return Response.status(Response.Status.OK).entity(response.salesTarget.toMap()).build()
    }

    @DELETE
    @Path("/{id}")
    @Operation(summary = "売上目標を削除する")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request = DeleteSalesTargetRequest.newBuilder().setId(id).build()
        grpc.withTenant(stub).deleteSalesTarget(request)
        return Response.noContent().build()
    }
}

data class UpsertSalesTargetBody(
    val storeId: String? = null,
    val targetMonth: String,
    val targetAmount: Long,
)
