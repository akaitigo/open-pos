package com.openpos.analytics.service

import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.LocalDate
import java.util.UUID

/**
 * 分析クエリサービス。
 * ABC分析・粗利レポート・売上予測の集計ロジックを提供する。
 */
@ApplicationScoped
class AnalyticsQueryService {
    @Inject
    lateinit var dailySalesRepository: DailySalesRepository

    @Inject
    lateinit var productSalesRepository: ProductSalesRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    // === ABC Analysis (#183) ===

    data class AbcItem(
        val productId: UUID,
        val productName: String,
        val revenue: Long,
        val revenueRatio: Double,
        val cumulativeRatio: Double,
        val rank: String,
    )

    fun getAbcAnalysis(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AbcItem> {
        tenantFilterService.enableFilter()
        val raw = productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate)

        // Group by productId and aggregate
        val grouped =
            raw
                .groupBy { it.productId }
                .map { (productId, records) ->
                    val name = records.first().productName
                    val totalRevenue = records.sumOf { it.totalAmount }
                    Triple(productId, name, totalRevenue)
                }.sortedByDescending { it.third }

        val totalRevenue = grouped.sumOf { it.third }
        if (totalRevenue == 0L) return emptyList()

        var cumulative = 0.0
        return grouped.map { (productId, name, revenue) ->
            val ratio = revenue.toDouble() / totalRevenue
            cumulative += ratio
            val rank =
                when {
                    cumulative <= 0.70 -> "A"
                    cumulative <= 0.90 -> "B"
                    else -> "C"
                }
            AbcItem(
                productId = productId,
                productName = name,
                revenue = revenue,
                revenueRatio = ratio,
                cumulativeRatio = cumulative,
                rank = rank,
            )
        }
    }

    // === Gross Profit (#184) ===

    data class GrossProfitItem(
        val productId: UUID,
        val productName: String,
        val revenue: Long,
        val cost: Long,
        val grossProfit: Long,
        val marginRate: Double,
        val quantitySold: Int,
    )

    data class GrossProfitReport(
        val items: List<GrossProfitItem>,
        val totalRevenue: Long,
        val totalCost: Long,
        val totalGrossProfit: Long,
    )

    fun getGrossProfitReport(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): GrossProfitReport {
        tenantFilterService.enableFilter()
        val raw = productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate)

        val grouped =
            raw
                .groupBy { it.productId }
                .map { (productId, records) ->
                    val name = records.first().productName
                    val totalRevenue = records.sumOf { it.totalAmount }
                    val totalCost = records.sumOf { it.costAmount }
                    val totalQty = records.sumOf { it.quantitySold }
                    val grossProfit = totalRevenue - totalCost
                    val marginRate = if (totalRevenue > 0) grossProfit.toDouble() / totalRevenue else 0.0
                    GrossProfitItem(
                        productId = productId,
                        productName = name,
                        revenue = totalRevenue,
                        cost = totalCost,
                        grossProfit = grossProfit,
                        marginRate = marginRate,
                        quantitySold = totalQty,
                    )
                }.sortedByDescending { it.grossProfit }

        return GrossProfitReport(
            items = grouped,
            totalRevenue = grouped.sumOf { it.revenue },
            totalCost = grouped.sumOf { it.cost },
            totalGrossProfit = grouped.sumOf { it.grossProfit },
        )
    }

    // === Sales Forecast (#182) ===

    data class ForecastPoint(
        val date: LocalDate,
        val actualAmount: Long,
        val movingAverage: Long,
    )

    fun getSalesForecast(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        windowDays: Int,
    ): List<ForecastPoint> {
        tenantFilterService.enableFilter()
        val dailySales = dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)

        val salesByDate = dailySales.associateBy { it.date }
        val allDates = generateSequence(startDate) { it.plusDays(1) }.takeWhile { !it.isAfter(endDate) }.toList()

        // Build list of daily amounts
        val dailyAmounts =
            allDates.map { date ->
                date to (salesByDate[date]?.grossAmount ?: 0L)
            }

        val window = if (windowDays > 0) windowDays else 7

        return dailyAmounts.mapIndexed { index, (date, amount) ->
            // Simple moving average
            val start = maxOf(0, index - window + 1)
            val windowSlice = dailyAmounts.subList(start, index + 1)
            val avg = windowSlice.sumOf { it.second } / windowSlice.size
            ForecastPoint(
                date = date,
                actualAmount = amount,
                movingAverage = avg,
            )
        }
    }
}
