package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.GetDailySalesRequest
import openpos.analytics.v1.GetHourlySalesRequest
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
    @Path("/daily")
    fun getDailySales(
        @QueryParam("storeId") storeId: String,
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): List<Map<String, Any?>> {
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
        return grpc
            .withTenant(stub)
            .getDailySales(request)
            .dailySalesList
            .map { it.toMap() }
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
        return grpc
            .withTenant(stub)
            .getSalesSummary(request)
            .summary
            .toMap()
    }

    @GET
    @Path("/hourly")
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
        return grpc
            .withTenant(stub)
            .getHourlySales(request)
            .hourlySalesList
            .map { it.toMap() }
    }
}
