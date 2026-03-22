package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class AnalyticsQueryServiceTest {
    @Inject
    lateinit var analyticsQueryService: AnalyticsQueryService

    @InjectMock
    lateinit var dailySalesRepository: DailySalesRepository

    @InjectMock
    lateinit var productSalesRepository: ProductSalesRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val startDate = LocalDate.of(2026, 3, 1)
    private val endDate = LocalDate.of(2026, 3, 31)

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === ABC Analysis ===

    @Nested
    inner class GetAbcAnalysis {
        @Test
        fun `returns ABC ranked items sorted by revenue descending`() {
            // Arrange
            val p1 = UUID.randomUUID()
            val p2 = UUID.randomUUID()
            val p3 = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(p1, "Product A", 100, 700000, 400000),
                    createProductSalesEntity(p2, "Product B", 50, 200000, 100000),
                    createProductSalesEntity(p3, "Product C", 10, 100000, 50000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getAbcAnalysis(storeId, startDate, endDate)

            // Assert
            assertEquals(3, result.size)
            assertEquals("A", result[0].rank)
            assertEquals("Product A", result[0].productName)
            assertEquals(700000L, result[0].revenue)
            assertEquals("B", result[1].rank)
            assertEquals("C", result[2].rank)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns empty list when total revenue is zero`() {
            // Arrange
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(emptyList())

            // Act
            val result = analyticsQueryService.getAbcAnalysis(storeId, startDate, endDate)

            // Assert
            assertTrue(result.isEmpty())
        }

        @Test
        fun `aggregates multiple records for the same product`() {
            // Arrange
            val productId = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(productId, "Product A", 10, 100000, 50000),
                    createProductSalesEntity(productId, "Product A", 20, 200000, 100000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getAbcAnalysis(storeId, startDate, endDate)

            // Assert
            assertEquals(1, result.size)
            assertEquals(300000L, result[0].revenue)
            assertEquals(1.0, result[0].revenueRatio, 0.01)
            assertEquals(1.0, result[0].cumulativeRatio, 0.01)
            assertEquals("C", result[0].rank)
        }

        @Test
        fun `assigns B rank when cumulative ratio is between 70 and 90 percent`() {
            // Arrange: 4 products with revenue split: 60%, 20%, 15%, 5%
            val p1 = UUID.randomUUID()
            val p2 = UUID.randomUUID()
            val p3 = UUID.randomUUID()
            val p4 = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(p1, "Big Seller", 100, 600000, 300000),
                    createProductSalesEntity(p2, "Medium Seller", 50, 200000, 100000),
                    createProductSalesEntity(p3, "Small Seller", 30, 150000, 75000),
                    createProductSalesEntity(p4, "Tiny Seller", 10, 50000, 25000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getAbcAnalysis(storeId, startDate, endDate)

            // Assert
            assertEquals(4, result.size)
            assertEquals("A", result[0].rank) // 60% cumulative
            assertEquals("B", result[1].rank) // 80% cumulative
            // p1=60% (cum 60% -> A), p2=20% (cum 80% -> B), p3=15% (cum 95% -> C), p4=5% (cum 100% -> C)
            assertEquals("C", result[2].rank)
            assertEquals("C", result[3].rank)
        }
    }

    // === Gross Profit Report ===

    @Nested
    inner class GetGrossProfitReport {
        @Test
        fun `returns gross profit report sorted by profit descending`() {
            // Arrange
            val p1 = UUID.randomUUID()
            val p2 = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(p1, "High Margin", 10, 500000, 100000),
                    createProductSalesEntity(p2, "Low Margin", 20, 300000, 250000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getGrossProfitReport(storeId, startDate, endDate)

            // Assert
            assertEquals(2, result.items.size)
            // High Margin: profit = 500000 - 100000 = 400000
            assertEquals(400000L, result.items[0].grossProfit)
            assertEquals("High Margin", result.items[0].productName)
            assertEquals(0.8, result.items[0].marginRate, 0.01)
            // Low Margin: profit = 300000 - 250000 = 50000
            assertEquals(50000L, result.items[1].grossProfit)
            assertEquals(800000L, result.totalRevenue)
            assertEquals(350000L, result.totalCost)
            assertEquals(450000L, result.totalGrossProfit)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns empty report when no data`() {
            // Arrange
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(emptyList())

            // Act
            val result = analyticsQueryService.getGrossProfitReport(storeId, startDate, endDate)

            // Assert
            assertTrue(result.items.isEmpty())
            assertEquals(0L, result.totalRevenue)
            assertEquals(0L, result.totalCost)
            assertEquals(0L, result.totalGrossProfit)
        }

        @Test
        fun `handles zero revenue product with zero margin rate`() {
            // Arrange
            val productId = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(productId, "Free Item", 5, 0, 0),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getGrossProfitReport(storeId, startDate, endDate)

            // Assert
            assertEquals(1, result.items.size)
            assertEquals(0.0, result.items[0].marginRate, 0.001)
            assertEquals(0L, result.items[0].grossProfit)
        }

        @Test
        fun `aggregates multiple records for same product`() {
            // Arrange
            val productId = UUID.randomUUID()
            val records =
                listOf(
                    createProductSalesEntity(productId, "Product A", 10, 100000, 50000),
                    createProductSalesEntity(productId, "Product A", 5, 60000, 30000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getGrossProfitReport(storeId, startDate, endDate)

            // Assert
            assertEquals(1, result.items.size)
            assertEquals(160000L, result.items[0].revenue)
            assertEquals(80000L, result.items[0].cost)
            assertEquals(80000L, result.items[0].grossProfit)
            assertEquals(15, result.items[0].quantitySold)
        }
    }

    // === Sales Forecast ===

    @Nested
    inner class GetSalesForecast {
        @Test
        fun `returns forecast with moving average`() {
            // Arrange: 7 days of daily sales
            val entities =
                (1..7).map { day ->
                    DailySalesEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        storeId = this@AnalyticsQueryServiceTest.storeId
                        date = LocalDate.of(2026, 3, day)
                        grossAmount = day * 10000L
                        transactionCount = day
                    }
                }
            val start = LocalDate.of(2026, 3, 1)
            val end = LocalDate.of(2026, 3, 7)
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, start, end))
                .thenReturn(entities)

            // Act
            val result = analyticsQueryService.getSalesForecast(storeId, start, end, 3)

            // Assert
            assertEquals(7, result.size)
            // Day 1: window [1], avg = 10000
            assertEquals(10000L, result[0].actualAmount)
            assertEquals(10000L, result[0].movingAverage)
            // Day 3: window [1,2,3], avg = (10000+20000+30000)/3 = 20000
            assertEquals(30000L, result[2].actualAmount)
            assertEquals(20000L, result[2].movingAverage)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns forecast with default window when windowDays is zero`() {
            // Arrange
            val start = LocalDate.of(2026, 3, 1)
            val end = LocalDate.of(2026, 3, 3)
            val entities =
                (1..3).map { day ->
                    DailySalesEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        storeId = this@AnalyticsQueryServiceTest.storeId
                        date = LocalDate.of(2026, 3, day)
                        grossAmount = 10000L
                        transactionCount = 1
                    }
                }
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, start, end))
                .thenReturn(entities)

            // Act — windowDays = 0 should default to 7
            val result = analyticsQueryService.getSalesForecast(storeId, start, end, 0)

            // Assert
            assertEquals(3, result.size)
            // With window=7, all 3 days are within window, so moving avg = 10000
            assertEquals(10000L, result[2].movingAverage)
        }

        @Test
        fun `fills zero for dates with no sales data`() {
            // Arrange: 3-day range, only middle day has data
            val start = LocalDate.of(2026, 3, 1)
            val end = LocalDate.of(2026, 3, 3)
            val entities =
                listOf(
                    DailySalesEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        storeId = this@AnalyticsQueryServiceTest.storeId
                        date = LocalDate.of(2026, 3, 2)
                        grossAmount = 30000L
                        transactionCount = 3
                    },
                )
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, start, end))
                .thenReturn(entities)

            // Act
            val result = analyticsQueryService.getSalesForecast(storeId, start, end, 3)

            // Assert
            assertEquals(3, result.size)
            assertEquals(0L, result[0].actualAmount)
            assertEquals(30000L, result[1].actualAmount)
            assertEquals(0L, result[2].actualAmount)
        }

        @Test
        fun `returns empty list for empty date range`() {
            // Arrange
            val start = LocalDate.of(2026, 3, 5)
            val end = LocalDate.of(2026, 3, 1) // end before start
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, start, end))
                .thenReturn(emptyList())

            // Act
            val result = analyticsQueryService.getSalesForecast(storeId, start, end, 7)

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    // === Helpers ===

    private fun createProductSalesEntity(
        productId: UUID,
        name: String,
        qty: Int,
        amount: Long,
        cost: Long,
    ): ProductSalesEntity =
        ProductSalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@AnalyticsQueryServiceTest.storeId
            this.productId = productId
            productName = name
            date = startDate
            quantitySold = qty
            totalAmount = amount
            costAmount = cost
            transactionCount = qty
        }
}
