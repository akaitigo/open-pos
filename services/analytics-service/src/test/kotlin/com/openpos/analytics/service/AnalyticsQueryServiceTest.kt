package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.repository.HourlySalesRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
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
    lateinit var analyticsService: AnalyticsService

    @InjectMock
    lateinit var hourlySalesRepository: HourlySalesRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class GetHourlySalesFromDb {
        @Test
        fun `returns 24 hours with real DB data merged`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 3, 15)
            val entities =
                listOf(
                    createHourlySalesEntity(saleDate, 9, 120000, 12),
                    createHourlySalesEntity(saleDate, 12, 250000, 25),
                    createHourlySalesEntity(saleDate, 18, 180000, 15),
                )
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(entities)

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            // Check specific hours with data
            assertEquals(120000L, result[9].totalSales)
            assertEquals(12, result[9].transactionCount)
            assertEquals(250000L, result[12].totalSales)
            assertEquals(25, result[12].transactionCount)
            assertEquals(180000L, result[18].totalSales)
            assertEquals(15, result[18].transactionCount)
            // Check hours without data
            assertEquals(0L, result[0].totalSales)
            assertEquals(0, result[0].transactionCount)
            assertEquals(0L, result[23].totalSales)
            assertEquals(0, result[23].transactionCount)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns all zeros for date with no data`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 1, 1)
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(emptyList())

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            result.forEachIndexed { index, hourly ->
                assertEquals(index, hourly.hour)
                assertEquals(0L, hourly.totalSales)
                assertEquals(0, hourly.transactionCount)
            }
        }

        @Test
        fun `handles all 24 hours having data`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 3, 15)
            val entities =
                (0..23).map { hour ->
                    createHourlySalesEntity(saleDate, hour, (hour + 1) * 10000L, hour + 1)
                }
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(entities)

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            result.forEachIndexed { index, hourly ->
                assertEquals(index, hourly.hour)
                assertEquals((index + 1) * 10000L, hourly.totalSales)
                assertEquals(index + 1, hourly.transactionCount)
            }
        }

        @Test
        fun `handles boundary values - zero and max amounts`() {
            // Arrange
            val saleDate = LocalDate.of(2026, 3, 15)
            val entities =
                listOf(
                    createHourlySalesEntity(saleDate, 0, 0, 0),
                    createHourlySalesEntity(saleDate, 23, Long.MAX_VALUE / 2, Int.MAX_VALUE / 2),
                )
            whenever(hourlySalesRepository.listByStoreAndDate(storeId, saleDate)).thenReturn(entities)

            // Act
            val result = analyticsService.getHourlySales(storeId, saleDate)

            // Assert
            assertEquals(24, result.size)
            assertEquals(0L, result[0].totalSales)
            assertEquals(0, result[0].transactionCount)
            assertEquals(Long.MAX_VALUE / 2, result[23].totalSales)
            assertEquals(Int.MAX_VALUE / 2, result[23].transactionCount)
        }
    }

    // === Helpers ===

    private fun createHourlySalesEntity(
        date: LocalDate,
        hour: Int,
        totalSales: Long,
        txnCount: Int,
    ): HourlySalesEntity =
        HourlySalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            storeId = this@AnalyticsQueryServiceTest.storeId
            saleDate = date
            this.hour = hour
            this.totalSales = totalSales
            transactionCount = txnCount
        }
}
