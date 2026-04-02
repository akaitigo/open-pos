package com.openpos.analytics.service

import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.math.BigDecimal
import java.math.RoundingMode
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

        val totalRevenueBd = BigDecimal.valueOf(totalRevenue)
        var cumulative = 0.0
        return grouped.map { (productId, name, revenue) ->
            val ratio = BigDecimal.valueOf(revenue).divide(totalRevenueBd, 10, RoundingMode.HALF_UP).toDouble()
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
                    val marginRate =
                        if (totalRevenue > 0) {
                            BigDecimal.valueOf(grossProfit).divide(BigDecimal.valueOf(totalRevenue), 10, RoundingMode.HALF_UP).toDouble()
                        } else {
                            0.0
                        }
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

    // === Category Sales Report (#1030) ===

    data class CategorySalesItem(
        val categoryId: UUID?,
        val categoryName: String,
        val totalAmount: Long,
        val quantitySold: Int,
        val transactionCount: Int,
    )

    /**
     * カテゴリ別売上レポートを取得する。
     * category_id + category_name の組でグループ化し、同名の異なるカテゴリを区別する (#1141)。
     *
     * transactionCount は概算値: product_sales テーブルは日別×商品別の粒度で
     * transaction_id を持たないため、正確なユニークトランザクション数は算出不可。
     * 日別の最大 transactionCount を合計することで、同カテゴリ内の商品数による
     * 水増しを防止している (#1148)。
     */
    fun getCategorySalesReport(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<CategorySalesItem> {
        tenantFilterService.enableFilter()
        val raw = productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate)

        return raw
            .groupBy { Pair(it.categoryId, it.categoryName.ifBlank { "\u672A\u5206\u985E" }) }
            .map { (key, records) ->
                // 日別にグループ化し、各日の最大 transactionCount を合計する（概算値）
                val estimatedTxCount =
                    records
                        .groupBy { it.date }
                        .values
                        .sumOf { dayRecords -> dayRecords.maxOf { it.transactionCount } }
                CategorySalesItem(
                    categoryId = key.first,
                    categoryName = key.second,
                    totalAmount = records.sumOf { it.totalAmount },
                    quantitySold = records.sumOf { it.quantitySold },
                    transactionCount = estimatedTxCount,
                )
            }.sortedByDescending { it.totalAmount }
    }
}
