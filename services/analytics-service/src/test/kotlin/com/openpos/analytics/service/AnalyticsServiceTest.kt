package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class AnalyticsServiceTest {
    @Inject
    lateinit var analyticsService: AnalyticsService

    @InjectMock
    lateinit var dailySalesRepository: DailySalesRepository

    @InjectMock
    lateinit var productSalesRepository: ProductSalesRepository

    @InjectMock
    lateinit var hourlySalesRepository: HourlySalesRepository

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

    @Nested
    inner class GetDailySales {
        @Test
        fun `returns daily sales for date range`() {
            // Arrange
            val dailyList =
                listOf(
                    createDailySalesEntity(LocalDate.of(2026, 3, 1), 50000, 3),
                    createDailySalesEntity(LocalDate.of(2026, 3, 2), 80000, 5),
                )
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)).thenReturn(dailyList)

            // Act
            val result = analyticsService.getDailySales(storeId, startDate, endDate)

            // Assert
            assertEquals(2, result.size)
            assertEquals(50000L, result[0].totalSales)
            assertEquals(80000L, result[1].totalSales)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns empty list when no data`() {
            // Arrange
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)).thenReturn(emptyList())

            // Act
            val result = analyticsService.getDailySales(storeId, startDate, endDate)

            // Assert
            assertEquals(0, result.size)
        }
    }

    @Nested
    inner class GetSalesSummary {
        @Test
        fun `aggregates daily sales into summary`() {
            // Arrange
            val dailyList =
                listOf(
                    createDailySalesEntity(LocalDate.of(2026, 3, 1), 50000, 3),
                    createDailySalesEntity(LocalDate.of(2026, 3, 2), 80000, 5),
                )
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)).thenReturn(dailyList)

            // Act
            val result = analyticsService.getSalesSummary(storeId, startDate, endDate)

            // Assert
            assertEquals(130000L, result.totalGross)
            assertEquals(8, result.totalTransactions)
            assertEquals(130000L / 8, result.averageTransaction)
        }

        @Test
        fun `returns zero summary when no data`() {
            // Arrange
            whenever(dailySalesRepository.listByStoreAndDateRange(storeId, startDate, endDate)).thenReturn(emptyList())

            // Act
            val result = analyticsService.getSalesSummary(storeId, startDate, endDate)

            // Assert
            assertEquals(0L, result.totalGross)
            assertEquals(0, result.totalTransactions)
            assertEquals(0L, result.averageTransaction)
        }
    }

    @Nested
    inner class GetProductSales {
        @Test
        fun `returns product sales with pagination`() {
            // Arrange
            val productId = UUID.randomUUID()
            val products =
                listOf(
                    createProductSalesEntity(productId, "Test Product", 10, 100000),
                )
            whenever(
                productSalesRepository.listByStoreAndDateRange(
                    eq(storeId),
                    eq(startDate),
                    eq(endDate),
                    isNull(),
                    isNull(),
                    any<Page>(),
                ),
            ).thenReturn(products)
            whenever(
                productSalesRepository.countByStoreAndDateRange(eq(storeId), eq(startDate), eq(endDate), isNull()),
            ).thenReturn(1L)

            // Act
            val (result, totalCount) =
                analyticsService.getProductSales(storeId, startDate, endDate, null, null, 0, 20)

            // Assert
            assertEquals(1, result.size)
            assertEquals(10, result[0].quantitySold)
            assertEquals(1L, totalCount)
        }

        @Test
        fun `returns empty list when no product sales`() {
            // Arrange
            whenever(
                productSalesRepository.listByStoreAndDateRange(
                    eq(storeId),
                    eq(startDate),
                    eq(endDate),
                    isNull(),
                    isNull(),
                    any<Page>(),
                ),
            ).thenReturn(emptyList())
            whenever(
                productSalesRepository.countByStoreAndDateRange(eq(storeId), eq(startDate), eq(endDate), isNull()),
            ).thenReturn(0L)

            // Act
            val (result, totalCount) =
                analyticsService.getProductSales(storeId, startDate, endDate, null, null, 0, 20)

            // Assert
            assertEquals(0, result.size)
            assertEquals(0L, totalCount)
        }
    }

    @Nested
    inner class GetHourlySales {
        @Test
        fun `returns 24 hours with existing data filled in`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 3, 15)
            val existingEntities =
                listOf(
                    createHourlySalesEntity(saleDate, 10, 30000, 5),
                    createHourlySalesEntity(saleDate, 12, 50000, 8),
                )
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(existingEntities)

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            assertEquals(0, result[0].hour)
            assertEquals(0L, result[0].totalSales)
            assertEquals(10, result[10].hour)
            assertEquals(30000L, result[10].totalSales)
            assertEquals(5, result[10].transactionCount)
            assertEquals(12, result[12].hour)
            assertEquals(50000L, result[12].totalSales)
            assertEquals(8, result[12].transactionCount)
            assertEquals(23, result[23].hour)
            assertEquals(0L, result[23].totalSales)
        }

        @Test
        fun `returns all zeros when no data`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 3, 15)
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(emptyList())

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            result.forEach { hourly ->
                assertEquals(0L, hourly.totalSales)
                assertEquals(0, hourly.transactionCount)
            }
        }
    }

    // === Helpers ===

    private fun createDailySalesEntity(
        date: LocalDate,
        totalSales: Long,
        txnCount: Int,
    ): DailySalesEntity =
        DailySalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@AnalyticsServiceTest.storeId
            saleDate = date
            this.totalSales = totalSales
            transactionCount = txnCount
            netSales = totalSales
            voidedCount = 0
            returnedCount = 0
        }

    private fun createProductSalesEntity(
        productId: UUID,
        name: String,
        qty: Int,
        amount: Long,
    ): ProductSalesEntity =
        ProductSalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@AnalyticsServiceTest.storeId
            this.productId = productId
            productName = name
            saleDate = startDate
            quantitySold = qty
            totalAmount = amount
            averagePrice = if (qty > 0) amount / qty else 0
            transactionCount = qty
        }

    private fun createHourlySalesEntity(
        date: LocalDate,
        hour: Int,
        totalSales: Long,
        txnCount: Int,
    ): HourlySalesEntity =
        HourlySalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@AnalyticsServiceTest.storeId
            saleDate = date
            this.hour = hour
            this.totalSales = totalSales
            transactionCount = txnCount
        }
}
