package com.openpos.analytics.grpc

import com.openpos.analytics.service.AnalyticsService
import com.openpos.analytics.service.HourlySalesResult
import io.grpc.Status
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.DailySales
import openpos.analytics.v1.GetDailySalesRequest
import openpos.analytics.v1.GetDailySalesResponse
import openpos.analytics.v1.GetHourlySalesRequest
import openpos.analytics.v1.GetHourlySalesResponse
import openpos.analytics.v1.GetProductSalesRequest
import openpos.analytics.v1.GetProductSalesResponse
import openpos.analytics.v1.GetSalesSummaryRequest
import openpos.analytics.v1.GetSalesSummaryResponse
import openpos.analytics.v1.HourlySales
import openpos.analytics.v1.ProductSales
import openpos.analytics.v1.SalesSummary
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationResponse
import java.time.LocalDate
import java.util.UUID

@GrpcService
@Blocking
class AnalyticsGrpcService : AnalyticsServiceGrpc.AnalyticsServiceImplBase() {
    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    override fun getDailySales(
        request: GetDailySalesRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetDailySalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val dateRange = requireDateRange(request.dateRange)
        val startDate = LocalDate.parse(dateRange.start)
        val endDate = LocalDate.parse(dateRange.end)

        val results = analyticsService.getDailySales(storeId, startDate, endDate)

        responseObserver.onNext(
            GetDailySalesResponse
                .newBuilder()
                .addAllDailySales(
                    results.map { entity ->
                        DailySales
                            .newBuilder()
                            .setDate(entity.saleDate.toString())
                            .setStoreId(entity.storeId.toString())
                            .setGrossAmount(entity.totalSales)
                            .setNetAmount(entity.netSales)
                            .setTaxAmount(entity.taxAmount)
                            .setDiscountAmount(entity.discountAmount)
                            .setTransactionCount(entity.transactionCount)
                            .setCashAmount(entity.cashAmount)
                            .setCardAmount(entity.cardAmount)
                            .setQrAmount(entity.qrAmount)
                            .build()
                    },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getSalesSummary(
        request: GetSalesSummaryRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetSalesSummaryResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val dateRange = requireDateRange(request.dateRange)
        val startDate = LocalDate.parse(dateRange.start)
        val endDate = LocalDate.parse(dateRange.end)

        val summary = analyticsService.getSalesSummary(storeId, startDate, endDate)

        responseObserver.onNext(
            GetSalesSummaryResponse
                .newBuilder()
                .setSummary(
                    SalesSummary
                        .newBuilder()
                        .setPeriod(dateRange)
                        .setTotalGross(summary.totalGross)
                        .setTotalNet(summary.totalNet)
                        .setTotalTax(summary.totalTax)
                        .setTotalDiscount(summary.totalDiscount)
                        .setTotalTransactions(summary.totalTransactions)
                        .setAverageTransaction(summary.averageTransaction)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getProductSales(
        request: GetProductSalesRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetProductSalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val dateRange = requireDateRange(request.dateRange)
        val startDate = LocalDate.parse(dateRange.start)
        val endDate = LocalDate.parse(dateRange.end)

        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize else 20
        val sortBy = request.sortBy.ifBlank { null }

        val (results, totalCount) =
            analyticsService.getProductSales(
                storeId = storeId,
                startDate = startDate,
                endDate = endDate,
                productId = null,
                sortBy = sortBy,
                page = page,
                pageSize = pageSize,
            )
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0

        responseObserver.onNext(
            GetProductSalesResponse
                .newBuilder()
                .addAllProductSales(
                    results.map { entity ->
                        ProductSales
                            .newBuilder()
                            .setProductId(entity.productId.toString())
                            .setProductName(entity.productName)
                            .setQuantitySold(entity.quantitySold)
                            .setTotalAmount(entity.totalAmount)
                            .setTransactionCount(entity.transactionCount)
                            .build()
                    },
                ).setPagination(
                    PaginationResponse
                        .newBuilder()
                        .setPage(page + 1)
                        .setPageSize(pageSize)
                        .setTotalCount(totalCount)
                        .setTotalPages(totalPages)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    override fun getHourlySales(
        request: GetHourlySalesRequest,
        responseObserver: io.grpc.stub.StreamObserver<GetHourlySalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val saleDate = LocalDate.parse(request.date)

        val results = analyticsService.getHourlySales(storeId, saleDate)

        responseObserver.onNext(
            GetHourlySalesResponse
                .newBuilder()
                .addAllHourlySales(
                    results.map { it.toProto() },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Utility Extensions ===

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun requireDateRange(dateRange: DateRange?): DateRange {
        if (dateRange == null || dateRange.start.isBlank() || dateRange.end.isBlank()) {
            throw Status.INVALID_ARGUMENT
                .withDescription("date_range with start and end is required")
                .asRuntimeException()
        }
        return dateRange
    }

    private fun HourlySalesResult.toProto(): HourlySales =
        HourlySales
            .newBuilder()
            .setHour(hour)
            .setAmount(totalSales)
            .setTransactionCount(transactionCount)
            .build()
}
