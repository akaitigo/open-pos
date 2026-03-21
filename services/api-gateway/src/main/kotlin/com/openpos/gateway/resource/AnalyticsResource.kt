package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.GetAbcAnalysisRequest
import openpos.analytics.v1.GetDailySalesRequest
import openpos.analytics.v1.GetGrossProfitReportRequest
import openpos.analytics.v1.GetHourlySalesRequest
import openpos.analytics.v1.GetSalesForecastRequest
import openpos.analytics.v1.GetSalesSummaryRequest
import openpos.common.v1.DateRange

@Path("/api/analytics")
@Blocking
class AnalyticsResource {
    @Inject
    @GrpcClient("analytics-service")
    lateinit var stub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @GET
    @Path("/daily-sales")
    fun getDailySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        val request =
            GetDailySalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getDailySales(request)
        return mapOf("data" to response.dailySalesList.map { it.toMap() })
    }

    @GET
    @Path("/summary")
    fun getSalesSummary(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any?> {
        val request =
            GetSalesSummaryRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getSalesSummary(request)
        val summary = response.summary
        return mapOf(
            "totalGross" to summary.totalGross,
            "totalNet" to summary.totalNet,
            "totalTax" to summary.totalTax,
            "totalDiscount" to summary.totalDiscount,
            "totalTransactions" to summary.totalTransactions,
            "averageTransaction" to summary.averageTransaction,
        )
    }

    @GET
    @Path("/hourly-sales")
    fun getHourlySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("date") date: String,
    ): List<Map<String, Any?>> {
        val request =
            GetHourlySalesRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDate(date)
                .build()
        val response = grpc.withTenant(stub).getHourlySales(request)
        return response.hourlySalesList.map { it.toMap() }
    }

    @GET
    @Path("/abc-analysis")
    fun getAbcAnalysis(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        val request =
            GetAbcAnalysisRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getAbcAnalysis(request)
        return mapOf("data" to response.itemsList.map { it.toMap() })
    }

    @GET
    @Path("/gross-profit")
    fun getGrossProfitReport(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> {
        val request =
            GetGrossProfitReportRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).getGrossProfitReport(request)
        return mapOf(
            "data" to response.itemsList.map { it.toMap() },
            "totalRevenue" to response.totalRevenue,
            "totalCost" to response.totalCost,
            "totalGrossProfit" to response.totalGrossProfit,
        )
    }

    @GET
    @Path("/sales-forecast")
    fun getSalesForecast(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
        @QueryParam("windowDays") @DefaultValue("7") windowDays: Int,
    ): Map<String, Any> {
        val request =
            GetSalesForecastRequest
                .newBuilder()
                .setStoreId(storeId)
                .setDateRange(
                    DateRange
                        .newBuilder()
                        .setStart(startDate)
                        .setEnd(endDate)
                        .build(),
                ).setWindowDays(windowDays)
                .build()
        val response = grpc.withTenant(stub).getSalesForecast(request)
        return mapOf("data" to response.dataPointsList.map { it.toMap() })
    }
}
