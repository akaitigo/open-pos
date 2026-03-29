package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.GetDailySalesRequest
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * 会計ソフト連携 REST リソース（#195 freee / MFクラウド）。
 * 既存の analytics/pos gRPC サービスを使って会計用データを返す。
 * RBAC: OWNER のみアクセス可能。
 */
@Path("/api/accounting")
@Blocking
@Timeout(30000)
class AccountingResource {
    @Inject
    @GrpcClient("analytics-service")
    lateinit var analyticsStub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub

    @Inject
    @GrpcClient("pos-service")
    lateinit var posStub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    @Path("/export/daily")
    fun exportDailySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("date") date: String,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER")
        val request =
            GetDailySalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(date)
                        .setEnd(date)
                        .build(),
                ).build()
        val response = grpc.withTenant(analyticsStub).getDailySales(request)
        return mapOf("data" to response.dailySalesList.map { it.toMap() })
    }

    @GET
    @Path("/export/transactions")
    fun exportTransactions(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER")
        val request =
            ListTransactionsRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(1)
                        .setPageSize(100)
                        .build(),
                ).build()
        val response = grpc.withTenant(posStub).listTransactions(request)
        return mapOf(
            "data" to response.transactionsList.map { it.toMap() },
            "pagination" to response.pagination.toMap(),
        )
    }
}
