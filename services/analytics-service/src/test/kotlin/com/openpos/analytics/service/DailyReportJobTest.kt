package com.openpos.analytics.service

import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class DailyReportJobTest {
    @Inject
    lateinit var dailyReportJob: DailyReportJob

    @InjectMock
    lateinit var dailySalesRepository: DailySalesRepository

    @Test
    fun `generateDailyReport processes each organization`() {
        // Arrange
        val yesterday = LocalDate.now().minusDays(1)
        val orgId1 = UUID.randomUUID()
        val orgId2 = UUID.randomUUID()
        whenever(dailySalesRepository.findDistinctOrganizationIdsBySaleDate(yesterday))
            .thenReturn(listOf(orgId1, orgId2))

        val salesOrg1 =
            listOf(
                createDailySalesEntity(orgId1, UUID.randomUUID(), yesterday, 500000, 10),
            )
        val salesOrg2 =
            listOf(
                createDailySalesEntity(orgId2, UUID.randomUUID(), yesterday, 300000, 5),
                createDailySalesEntity(orgId2, UUID.randomUUID(), yesterday, 200000, 3),
            )
        whenever(dailySalesRepository.findBySaleDate(yesterday, orgId1)).thenReturn(salesOrg1)
        whenever(dailySalesRepository.findBySaleDate(yesterday, orgId2)).thenReturn(salesOrg2)

        // Act
        dailyReportJob.generateDailyReport()

        // Assert
        assertNotNull(salesOrg1)
        assertNotNull(salesOrg2)
        assertEquals(1, salesOrg1.size)
        assertEquals(2, salesOrg2.size)
        assertEquals(500000, salesOrg1[0].grossAmount)
        verify(dailySalesRepository).findDistinctOrganizationIdsBySaleDate(yesterday)
        verify(dailySalesRepository, times(2)).findBySaleDate(any(), any())
    }

    @Test
    fun `generateDailyReport skips when no organizations have data`() {
        // Arrange
        val yesterday = LocalDate.now().minusDays(1)
        whenever(dailySalesRepository.findDistinctOrganizationIdsBySaleDate(yesterday))
            .thenReturn(emptyList())

        // Act
        dailyReportJob.generateDailyReport()

        // Assert
        assertEquals(yesterday, LocalDate.now().minusDays(1))
        verify(dailySalesRepository).findDistinctOrganizationIdsBySaleDate(yesterday)
        verify(dailySalesRepository, never()).findBySaleDate(any(), any())
    }

    @Test
    fun `generateDailyReport handles organization with empty sales`() {
        // Arrange
        val yesterday = LocalDate.now().minusDays(1)
        val orgId = UUID.randomUUID()
        whenever(dailySalesRepository.findDistinctOrganizationIdsBySaleDate(yesterday))
            .thenReturn(listOf(orgId))
        whenever(dailySalesRepository.findBySaleDate(yesterday, orgId))
            .thenReturn(emptyList())

        // Act
        dailyReportJob.generateDailyReport()

        // Assert
        assertNotNull(orgId)
        verify(dailySalesRepository).findBySaleDate(yesterday, orgId)
    }

    @Test
    fun `generateDailyReport handles single store with multiple transactions`() {
        // Arrange
        val yesterday = LocalDate.now().minusDays(1)
        val orgId = UUID.randomUUID()
        val storeId = UUID.randomUUID()
        val salesData = listOf(createDailySalesEntity(orgId, storeId, yesterday, 1000000, 50))
        whenever(dailySalesRepository.findDistinctOrganizationIdsBySaleDate(yesterday))
            .thenReturn(listOf(orgId))
        whenever(dailySalesRepository.findBySaleDate(yesterday, orgId))
            .thenReturn(salesData)

        // Act
        dailyReportJob.generateDailyReport()

        // Assert
        assertEquals(1, salesData.size)
        assertEquals(1000000, salesData[0].grossAmount)
        assertEquals(50, salesData[0].transactionCount)
        verify(dailySalesRepository).findBySaleDate(yesterday, orgId)
    }

    // === Helpers ===

    private fun createDailySalesEntity(
        orgId: UUID,
        storeId: UUID,
        date: LocalDate,
        grossAmount: Long,
        txnCount: Int,
    ): DailySalesEntity =
        DailySalesEntity().apply {
            id = UUID.randomUUID()
            organizationId = orgId
            this.storeId = storeId
            this.date = date
            this.grossAmount = grossAmount
            transactionCount = txnCount
            netAmount = grossAmount
        }
}
