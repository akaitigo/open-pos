package com.openpos.analytics.service

import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.LocalDate
import java.util.UUID

/**
 * 売上分析のビジネスロジック層。
 * 日次売上、商品別売上、時間帯別売上、サマリーの取得を提供する。
 */
@ApplicationScoped
class AnalyticsService {
    @Inject
    lateinit var dailySalesRepository: DailySalesRepository

    @Inject
    lateinit var productSalesRepository: ProductSalesRepository

    @Inject
    lateinit var hourlySalesRepository: HourlySalesRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    /**
     * 店舗の日次売上を日付範囲で取得する。
     */
    fun getDailySales(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<DailySalesEntity> {
        tenantFilterService.enableFilter()
        return dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)
    }

    /**
     * 店舗の売上サマリーを日付範囲で取得する。
     * 日次売上を集約してサマリーを返す。
     */
    fun getSalesSummary(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): SalesSummaryResult {
        tenantFilterService.enableFilter()
        val dailyList = dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)

        val totalGross = dailyList.sumOf { it.grossAmount }
        val totalTax = dailyList.sumOf { it.taxAmount }
        val totalDiscount = dailyList.sumOf { it.discountAmount }
        val totalNet = totalGross - totalTax - totalDiscount
        val totalTransactions = dailyList.sumOf { it.transactionCount }
        val averageTransaction = if (totalTransactions > 0) totalGross / totalTransactions.toLong() else 0L

        return SalesSummaryResult(
            totalGross = totalGross,
            totalNet = totalNet,
            totalTax = totalTax,
            totalDiscount = totalDiscount,
            totalTransactions = totalTransactions,
            averageTransaction = averageTransaction,
        )
    }

    /**
     * 商品別売上を日付範囲で取得する（ページネーション対応）。
     */
    fun getProductSales(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
        sortBy: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<ProductSalesEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val results = productSalesRepository.listByStoreAndDateRange(storeId, startDate, endDate, productId, sortBy, panachePage)
        val totalCount = productSalesRepository.countByStoreAndDateRange(storeId, startDate, endDate, productId)
        return Pair(results, totalCount)
    }

    /**
     * 特定日の時間帯別売上を取得する。
     * 全24時間帯分のデータを返す（取引ゼロの時間帯も含む）。
     */
    fun getHourlySales(
        storeId: UUID,
        saleDate: LocalDate,
    ): List<HourlySalesResult> {
        tenantFilterService.enableFilter()
        val existing = hourlySalesRepository.listByStoreAndDate(storeId, saleDate)
        val hourMap = existing.associateBy { it.hour }

        return (0..23).map { hour ->
            val entity = hourMap[hour]
            HourlySalesResult(
                hour = hour,
                totalSales = entity?.totalSales ?: 0L,
                transactionCount = entity?.transactionCount ?: 0,
            )
        }
    }
}

data class SalesSummaryResult(
    val totalGross: Long,
    val totalNet: Long,
    val totalTax: Long,
    val totalDiscount: Long,
    val totalTransactions: Int,
    val averageTransaction: Long,
)

data class HourlySalesResult(
    val hour: Int,
    val totalSales: Long,
    val transactionCount: Int,
)
