package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.paginatedResponse
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.GetDailySalesRequest
import openpos.analytics.v1.GetHourlySalesRequest
import openpos.analytics.v1.GetProductSalesRequest
import openpos.analytics.v1.GetSalesSummaryRequest
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest

@Path("/api/analytics")
@Blocking
class AnalyticsResource {
    @Inject
    @GrpcClient("analytics-service")
    lateinit var stub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @GET
    @Path("/daily")
    fun getDailySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("from") from: String,
        @QueryParam("to") to: String,
    ): Map<String, Any> {
        val request =
            GetDailySalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(from)
                        .setEnd(to)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getDailySales(request)
        return mapOf(
            "data" to
                response.dailySalesList.map { daily ->
                    mapOf(
                        "date" to daily.date,
                        "storeId" to daily.storeId,
                        "grossAmount" to daily.grossAmount,
                        "netAmount" to daily.netAmount,
                        "taxAmount" to daily.taxAmount,
                        "discountAmount" to daily.discountAmount,
                        "transactionCount" to daily.transactionCount,
                        "cashAmount" to daily.cashAmount,
                        "cardAmount" to daily.cardAmount,
                        "qrAmount" to daily.qrAmount,
                    )
                },
        )
    }

    @GET
    @Path("/summary")
    fun getSalesSummary(
        @QueryParam("storeId") storeId: String,
        @QueryParam("from") from: String,
        @QueryParam("to") to: String,
    ): Map<String, Any> {
        val request =
            GetSalesSummaryRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(from)
                        .setEnd(to)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getSalesSummary(request)
        val summary = response.summary
        return mapOf(
            "data" to
                mapOf(
                    "period" to
                        mapOf(
                            "start" to summary.period.start,
                            "end" to summary.period.end,
                        ),
                    "totalGross" to summary.totalGross,
                    "totalNet" to summary.totalNet,
                    "totalTax" to summary.totalTax,
                    "totalDiscount" to summary.totalDiscount,
                    "totalTransactions" to summary.totalTransactions,
                    "averageTransaction" to summary.averageTransaction,
                ),
        )
    }

    @GET
    @Path("/products")
    fun getProductSales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("from") from: String,
        @QueryParam("to") to: String,
        @QueryParam("sortBy") sortBy: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        val request =
            GetProductSalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(from)
                        .setEnd(to)
                        .build(),
                ).setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).apply {
                    sortBy?.let { setSortBy(it) }
                }.build()
        val response = grpc.withTenant(stub).getProductSales(request)
        return paginatedResponse(
            data =
                response.productSalesList.map { ps ->
                    mapOf(
                        "productId" to ps.productId,
                        "productName" to ps.productName,
                        "quantitySold" to ps.quantitySold,
                        "totalAmount" to ps.totalAmount,
                        "transactionCount" to ps.transactionCount,
                    )
                },
            pagination = response.pagination,
        )
    }

    @GET
    @Path("/hourly")
    fun getHourlySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("date") date: String,
    ): Map<String, Any> {
        val request =
            GetHourlySalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDate(date)
                .build()
        val response = grpc.withTenant(stub).getHourlySales(request)
        return mapOf(
            "data" to
                response.hourlySalesList.map { hs ->
                    mapOf(
                        "hour" to hs.hour,
                        "amount" to hs.amount,
                        "transactionCount" to hs.transactionCount,
                    )
                },
        )
    }
}
