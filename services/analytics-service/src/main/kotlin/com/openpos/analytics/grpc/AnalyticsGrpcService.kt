package com.openpos.analytics.grpc

import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import com.openpos.analytics.service.AnalyticsQueryService
import com.openpos.analytics.service.AnalyticsService
import com.openpos.analytics.service.SalesTargetService
import io.grpc.Status
import io.grpc.stub.StreamObserver
import io.quarkus.grpc.GrpcService
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import openpos.analytics.v1.AbcAnalysisItem
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.DailySales
import openpos.analytics.v1.DeleteSalesTargetRequest
import openpos.analytics.v1.DeleteSalesTargetResponse
import openpos.analytics.v1.CategorySalesItem
import openpos.analytics.v1.GetAbcAnalysisRequest
import openpos.analytics.v1.GetCategorySalesReportRequest
import openpos.analytics.v1.GetCategorySalesReportResponse
import openpos.analytics.v1.GetAbcAnalysisResponse
import openpos.analytics.v1.GetDailySalesRequest
import openpos.analytics.v1.GetDailySalesResponse
import openpos.analytics.v1.GetGrossProfitReportRequest
import openpos.analytics.v1.GetGrossProfitReportResponse
import openpos.analytics.v1.GetHourlySalesRequest
import openpos.analytics.v1.GetHourlySalesResponse
import openpos.analytics.v1.GetProductSalesRequest
import openpos.analytics.v1.GetProductSalesResponse
import openpos.analytics.v1.GetSalesForecastRequest
import openpos.analytics.v1.GetSalesForecastResponse
import openpos.analytics.v1.GetSalesSummaryRequest
import openpos.analytics.v1.GetSalesSummaryResponse
import openpos.analytics.v1.GetSalesTargetRequest
import openpos.analytics.v1.GetSalesTargetResponse
import openpos.analytics.v1.GrossProfitItem
import openpos.analytics.v1.HourlySales
import openpos.analytics.v1.ListSalesTargetsRequest
import openpos.analytics.v1.ListSalesTargetsResponse
import openpos.analytics.v1.ProductSales
import openpos.analytics.v1.SalesForecastPoint
import openpos.analytics.v1.SalesSummary
import openpos.analytics.v1.SalesTarget
import openpos.analytics.v1.UpsertSalesTargetRequest
import openpos.analytics.v1.UpsertSalesTargetResponse
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationResponse
import java.time.LocalDate
import java.util.UUID

@GrpcService
@Blocking
class AnalyticsGrpcService : AnalyticsServiceGrpc.AnalyticsServiceImplBase() {
    @Inject
    lateinit var tenantHelper: GrpcTenantHelper

    @Inject
    lateinit var dailySalesRepository: DailySalesRepository

    @Inject
    lateinit var productSalesRepository: ProductSalesRepository

    @Inject
    lateinit var analyticsQueryService: AnalyticsQueryService

    @Inject
    lateinit var analyticsService: AnalyticsService

    @Inject
    lateinit var hourlySalesRepository: HourlySalesRepository

    @Inject
    lateinit var salesTargetService: SalesTargetService

    // === Daily Sales ===

    override fun getDailySales(
        request: GetDailySalesRequest,
        responseObserver: StreamObserver<GetDailySalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val entities = dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)
        responseObserver.onNext(
            GetDailySalesResponse
                .newBuilder()
                .addAllDailySales(
                    entities.map { e ->
                        DailySales
                            .newBuilder()
                            .setDate(e.date.toString())
                            .setStoreId(e.storeId.toString())
                            .setGrossAmount(e.grossAmount)
                            .setNetAmount(e.netAmount)
                            .setTaxAmount(e.taxAmount)
                            .setDiscountAmount(e.discountAmount)
                            .setTransactionCount(e.transactionCount)
                            .setCashAmount(e.cashAmount)
                            .setCardAmount(e.cardAmount)
                            .setQrAmount(e.qrAmount)
                            .build()
                    },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Product Sales ===

    override fun getProductSales(
        request: GetProductSalesRequest,
        responseObserver: StreamObserver<GetProductSalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val raw = productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate)

        // Group by productId and aggregate
        val grouped =
            raw
                .groupBy { it.productId }
                .map { (productId, records) ->
                    val name = records.first().productName
                    val totalQty = records.sumOf { it.quantitySold }
                    val totalAmt = records.sumOf { it.totalAmount }
                    val txCount = records.sumOf { it.transactionCount }
                    ProductSales
                        .newBuilder()
                        .setProductId(productId.toString())
                        .setProductName(name)
                        .setQuantitySold(totalQty)
                        .setTotalAmount(totalAmt)
                        .setTransactionCount(txCount)
                        .build()
                }

        // Sort
        val sorted =
            when (request.sortBy) {
                "quantity" -> grouped.sortedByDescending { it.quantitySold }
                else -> grouped.sortedByDescending { it.totalAmount }
            }

        val page = if (request.hasPagination()) request.pagination.page - 1 else 0
        val pageSize = if (request.hasPagination() && request.pagination.pageSize > 0) request.pagination.pageSize.coerceAtMost(100) else 20
        val totalCount = sorted.size.toLong()
        val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0
        val paged = sorted.drop(page * pageSize).take(pageSize)

        responseObserver.onNext(
            GetProductSalesResponse
                .newBuilder()
                .addAllProductSales(paged)
                .setPagination(
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

    // === Hourly Sales ===

    override fun getHourlySales(
        request: GetHourlySalesRequest,
        responseObserver: StreamObserver<GetHourlySalesResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val saleDate =
            try {
                LocalDate.parse(request.date)
            } catch (e: Exception) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid date: ${request.date}").asRuntimeException()
            }

        val hourlyResults = analyticsService.getHourlySales(storeId, saleDate)

        val hourlyList =
            hourlyResults.map { result ->
                HourlySales
                    .newBuilder()
                    .setHour(result.hour)
                    .setAmount(result.totalSales)
                    .setTransactionCount(result.transactionCount)
                    .build()
            }
        responseObserver.onNext(
            GetHourlySalesResponse
                .newBuilder()
                .addAllHourlySales(hourlyList)
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Sales Summary ===

    override fun getSalesSummary(
        request: GetSalesSummaryRequest,
        responseObserver: StreamObserver<GetSalesSummaryResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val dailySales = dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)

        val totalGross = dailySales.sumOf { it.grossAmount }
        val totalNet = dailySales.sumOf { it.netAmount }
        val totalTax = dailySales.sumOf { it.taxAmount }
        val totalDiscount = dailySales.sumOf { it.discountAmount }
        val totalTx = dailySales.sumOf { it.transactionCount }
        val avgTx = if (totalTx > 0) totalGross / totalTx.toLong() else 0L

        responseObserver.onNext(
            GetSalesSummaryResponse
                .newBuilder()
                .setSummary(
                    SalesSummary
                        .newBuilder()
                        .setPeriod(
                            DateRange
                                .newBuilder()
                                .setStart(startDate.toString())
                                .setEnd(endDate.toString())
                                .build(),
                        ).setTotalGross(totalGross)
                        .setTotalNet(totalNet)
                        .setTotalTax(totalTax)
                        .setTotalDiscount(totalDiscount)
                        .setTotalTransactions(totalTx)
                        .setAverageTransaction(avgTx)
                        .build(),
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === ABC Analysis (#183) ===

    override fun getAbcAnalysis(
        request: GetAbcAnalysisRequest,
        responseObserver: StreamObserver<GetAbcAnalysisResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val items = analyticsQueryService.getAbcAnalysis(storeId, startDate, endDate)

        responseObserver.onNext(
            GetAbcAnalysisResponse
                .newBuilder()
                .addAllItems(
                    items.map { item ->
                        AbcAnalysisItem
                            .newBuilder()
                            .setProductId(item.productId.toString())
                            .setProductName(item.productName)
                            .setRevenue(item.revenue)
                            .setRevenueRatio(item.revenueRatio)
                            .setCumulativeRatio(item.cumulativeRatio)
                            .setRank(item.rank)
                            .build()
                    },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Gross Profit Report (#184) ===

    override fun getGrossProfitReport(
        request: GetGrossProfitReportRequest,
        responseObserver: StreamObserver<GetGrossProfitReportResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val report = analyticsQueryService.getGrossProfitReport(storeId, startDate, endDate)

        responseObserver.onNext(
            GetGrossProfitReportResponse
                .newBuilder()
                .addAllItems(
                    report.items.map { item ->
                        GrossProfitItem
                            .newBuilder()
                            .setProductId(item.productId.toString())
                            .setProductName(item.productName)
                            .setRevenue(item.revenue)
                            .setCost(item.cost)
                            .setGrossProfit(item.grossProfit)
                            .setMarginRate(item.marginRate)
                            .setQuantitySold(item.quantitySold)
                            .build()
                    },
                ).setTotalRevenue(report.totalRevenue)
                .setTotalCost(report.totalCost)
                .setTotalGrossProfit(report.totalGrossProfit)
                .build(),
        )
        responseObserver.onCompleted()
    }

    // === Sales Forecast (#182) ===

    override fun getSalesForecast(
        request: GetSalesForecastRequest,
        responseObserver: StreamObserver<GetSalesForecastResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val windowDays = if (request.windowDays > 0) request.windowDays else 7
        val points = analyticsQueryService.getSalesForecast(storeId, startDate, endDate, windowDays)

        responseObserver.onNext(
            GetSalesForecastResponse
                .newBuilder()
                .addAllDataPoints(
                    points.map { p ->
                        SalesForecastPoint
                            .newBuilder()
                            .setDate(p.date.toString())
                            .setActualAmount(p.actualAmount)
                            .setMovingAverage(p.movingAverage)
                            .build()
                    },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    // === Sales Targets ===

    override fun listSalesTargets(
        request: ListSalesTargetsRequest,
        responseObserver: StreamObserver<ListSalesTargetsResponse>,
    ) {
        tenantHelper.setupTenantContext()
        var targets = salesTargetService.listAll()

        // Apply store_id filter
        if (request.storeId.isNotEmpty()) {
            val storeId = request.storeId.toUUID()
            targets = targets.filter { it.storeId == storeId }
        }

        // Apply month filter
        if (request.month.isNotEmpty()) {
            val month =
                try {
                    LocalDate.parse(request.month)
                } catch (e: Exception) {
                    throw Status.INVALID_ARGUMENT.withDescription("Invalid month: ${request.month}").asRuntimeException()
                }
            targets = targets.filter { it.targetMonth == month }
        }

        responseObserver.onNext(
            ListSalesTargetsResponse
                .newBuilder()
                .addAllSalesTargets(targets.map { it.toProto() })
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun getSalesTarget(
        request: GetSalesTargetRequest,
        responseObserver: StreamObserver<GetSalesTargetResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val id = request.id.toUUID()
        val entity =
            salesTargetService.findById(id)
                ?: throw Status.NOT_FOUND.withDescription("Sales target not found: ${request.id}").asRuntimeException()

        responseObserver.onNext(
            GetSalesTargetResponse
                .newBuilder()
                .setSalesTarget(entity.toProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun upsertSalesTarget(
        request: UpsertSalesTargetRequest,
        responseObserver: StreamObserver<UpsertSalesTargetResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = if (request.storeId.isNotEmpty()) request.storeId.toUUID() else null
        val targetMonth =
            try {
                LocalDate.parse(request.targetMonth)
            } catch (e: Exception) {
                throw Status.INVALID_ARGUMENT
                    .withDescription("Invalid target_month: ${request.targetMonth}")
                    .asRuntimeException()
            }
        if (request.targetAmount <= 0) {
            throw Status.INVALID_ARGUMENT
                .withDescription("target_amount must be positive")
                .asRuntimeException()
        }

        val entity = salesTargetService.upsert(storeId, targetMonth, request.targetAmount)

        responseObserver.onNext(
            UpsertSalesTargetResponse
                .newBuilder()
                .setSalesTarget(entity.toProto())
                .build(),
        )
        responseObserver.onCompleted()
    }

    override fun deleteSalesTarget(
        request: DeleteSalesTargetRequest,
        responseObserver: StreamObserver<DeleteSalesTargetResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val id = request.id.toUUID()
        val deleted = salesTargetService.delete(id)
        if (!deleted) {
            throw Status.NOT_FOUND.withDescription("Sales target not found: ${request.id}").asRuntimeException()
        }

        responseObserver.onNext(DeleteSalesTargetResponse.getDefaultInstance())
        responseObserver.onCompleted()
    }

    // === Utility Extensions ===

    private fun com.openpos.analytics.entity.SalesTargetEntity.toProto(): SalesTarget =
        SalesTarget
            .newBuilder()
            .setId(id.toString())
            .setStoreId(storeId?.toString().orEmpty())
            .setTargetMonth(targetMonth.toString())
            .setTargetAmount(targetAmount)
            .setOrganizationId(organizationId.toString())
            .setCreatedAt(createdAt.toString())
            .setUpdatedAt(updatedAt.toString())
            .build()

    override fun getCategorySalesReport(
        request: GetCategorySalesReportRequest,
        responseObserver: StreamObserver<GetCategorySalesReportResponse>,
    ) {
        tenantHelper.setupTenantContext()
        val storeId = request.storeId.toUUID()
        val (startDate, endDate) = request.dateRange.toDates()
        val items = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

        responseObserver.onNext(
            GetCategorySalesReportResponse
                .newBuilder()
                .addAllItems(
                    items.map { item ->
                        CategorySalesItem
                            .newBuilder()
                            .setCategoryName(item.categoryName)
                            .setTotalAmount(item.totalAmount)
                            .setQuantitySold(item.quantitySold)
                            .setTransactionCount(item.transactionCount)
                            .build()
                    },
                ).build(),
        )
        responseObserver.onCompleted()
    }

    private fun String.toUUID(): UUID =
        try {
            UUID.fromString(this)
        } catch (e: IllegalArgumentException) {
            throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
        }

    private fun DateRange.toDates(): Pair<LocalDate, LocalDate> {
        val s =
            try {
                LocalDate.parse(start)
            } catch (e: Exception) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid start date: $start").asRuntimeException()
            }
        val e =
            try {
                LocalDate.parse(end)
            } catch (ex: Exception) {
                throw Status.INVALID_ARGUMENT.withDescription("Invalid end date: $end").asRuntimeException()
            }
        return Pair(s, e)
    }
}
