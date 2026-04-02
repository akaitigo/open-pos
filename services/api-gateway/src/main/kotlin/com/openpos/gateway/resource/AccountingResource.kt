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
    companion object {
        private val DATE_ONLY_REGEX = Regex("""\d{4}-\d{2}-\d{2}""")

        /**
         * YYYY-MM-DD 形式の日付文字列を RFC 3339 タイムスタンプに正規化する。
         * 既に RFC 3339（T を含む）形式の場合はそのまま返す。
         */
        fun normalizeStart(date: String): String = if (DATE_ONLY_REGEX.matches(date)) "${date}T00:00:00Z" else date

        fun normalizeEnd(date: String): String = if (DATE_ONLY_REGEX.matches(date)) "${date}T23:59:59.999Z" else date
    }

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
        val allTransactions = mutableListOf<openpos.pos.v1.Transaction>()
        var page = 1
        do {
            val request =
                ListTransactionsRequest
                    .newBuilder()
                    .setStoreId(storeId)
                    .setDateRange(
                        DateRange
                            .newBuilder()
                            .setStart(normalizeStart(startDate))
                            .setEnd(normalizeEnd(endDate))
                            .build(),
                    ).setPagination(
                        PaginationRequest
                            .newBuilder()
                            .setPage(page)
                            .setPageSize(100)
                            .build(),
                    ).build()
            val response = grpc.withTenant(posStub).listTransactions(request)
            allTransactions.addAll(response.transactionsList)
            page++
        } while (allTransactions.size < response.pagination.totalCount)
        return mapOf("data" to allTransactions.map { it.toMap() })
    }
}
