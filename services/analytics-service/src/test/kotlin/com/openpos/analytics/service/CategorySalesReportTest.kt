package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class CategorySalesReportTest {
    @Inject
    lateinit var analyticsQueryService: AnalyticsQueryService

    @InjectMock
    lateinit var productSalesRepository: ProductSalesRepository

    @InjectMock
    lateinit var dailySalesRepository: DailySalesRepository

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
    inner class GetCategorySalesReport {
        @Test
        fun `returns category sales grouped by category id and name`() {
            // Arrange
            val beveragesId = UUID.randomUUID()
            val foodId = UUID.randomUUID()
            val records =
                listOf(
                    createEntity(UUID.randomUUID(), "Coffee", beveragesId, "Beverages", 100, 500000),
                    createEntity(UUID.randomUUID(), "Tea", beveragesId, "Beverages", 50, 200000),
                    createEntity(UUID.randomUUID(), "Cake", foodId, "Food", 30, 300000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert
            assertEquals(2, result.size)
            assertEquals("Beverages", result[0].categoryName)
            assertEquals(beveragesId, result[0].categoryId)
            assertEquals(700000L, result[0].totalAmount)
            assertEquals(150, result[0].quantitySold)
            assertEquals("Food", result[1].categoryName)
            assertEquals(foodId, result[1].categoryId)
            assertEquals(300000L, result[1].totalAmount)
        }

        @Test
        fun `separates categories with same name but different ids`() {
            // Arrange - two distinct categories that happen to share the same name
            val catIdA = UUID.randomUUID()
            val catIdB = UUID.randomUUID()
            val records =
                listOf(
                    createEntity(UUID.randomUUID(), "Product X", catIdA, "Drinks", 10, 100000),
                    createEntity(UUID.randomUUID(), "Product Y", catIdB, "Drinks", 20, 200000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert - should be 2 separate entries, not merged
            assertEquals(2, result.size)
            val ids = result.map { it.categoryId }.toSet()
            assertTrue(ids.contains(catIdA))
            assertTrue(ids.contains(catIdB))
        }

        @Test
        fun `groups uncategorized products under default name`() {
            // Arrange
            val records =
                listOf(
                    createEntity(UUID.randomUUID(), "Product A", null, "", 10, 100000),
                    createEntity(UUID.randomUUID(), "Product B", null, "", 20, 200000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert
            assertEquals(1, result.size)
            assertNull(result[0].categoryId)
            assertEquals(300000L, result[0].totalAmount)
            assertEquals(30, result[0].quantitySold)
        }

        @Test
        fun `transactionCount uses daily max to avoid inflation from multiple products`() {
            // Arrange: 2 products in same category on same day — transactionCount should be max, not sum
            val catId = UUID.randomUUID()
            val records =
                listOf(
                    createEntity(UUID.randomUUID(), "Coffee", catId, "Beverages", 100, 500000),
                    createEntity(UUID.randomUUID(), "Tea", catId, "Beverages", 50, 200000),
                )
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(records)

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert: transactionCount = max(100, 50) = 100, not sum(100 + 50) = 150
            assertEquals(1, result.size)
            assertEquals(100, result[0].transactionCount)
            assertEquals(150, result[0].quantitySold) // quantitySold is still summed
        }

        @Test
        fun `transactionCount sums daily max across multiple days`() {
            // Arrange: same category, different days
            val catId = UUID.randomUUID()
            val day1Product =
                ProductSalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    storeId = this@CategorySalesReportTest.storeId
                    productId = UUID.randomUUID()
                    productName = "Coffee"
                    categoryId = catId
                    categoryName = "Beverages"
                    date = LocalDate.of(2026, 3, 1)
                    quantitySold = 10
                    totalAmount = 100000
                    costAmount = 0
                    transactionCount = 8
                }
            val day2Product =
                ProductSalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    storeId = this@CategorySalesReportTest.storeId
                    productId = UUID.randomUUID()
                    productName = "Tea"
                    categoryId = catId
                    categoryName = "Beverages"
                    date = LocalDate.of(2026, 3, 2)
                    quantitySold = 5
                    totalAmount = 50000
                    costAmount = 0
                    transactionCount = 3
                }
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(listOf(day1Product, day2Product))

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert: transactionCount = max(8) + max(3) = 11
            assertEquals(1, result.size)
            assertEquals(11, result[0].transactionCount)
        }

        @Test
        fun `returns empty list when no data`() {
            // Arrange
            whenever(productSalesRepository.findAggregatedByStoreAndDateRange(storeId, startDate, endDate))
                .thenReturn(emptyList())

            // Act
            val result = analyticsQueryService.getCategorySalesReport(storeId, startDate, endDate)

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    private fun createEntity(
        productId: UUID,
        name: String,
        categoryId: UUID?,
        categoryName: String,
        qty: Int,
        amount: Long,
    ): ProductSalesEntity =
        ProductSalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@CategorySalesReportTest.storeId
            this.productId = productId
            productName = name
            this.categoryId = categoryId
            this.categoryName = categoryName
            date = startDate
            quantitySold = qty
            totalAmount = amount
            costAmount = 0
            transactionCount = qty
        }
}
