package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.SalesTargetEntity
import com.openpos.analytics.repository.SalesTargetRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

/**
 * SalesTargetService の純粋ユニットテスト。
 * CDI プロキシを回避して JaCoCo カバレッジを確保する。
 */
class SalesTargetServiceUnitTest {
    private lateinit var service: SalesTargetService
    private lateinit var salesTargetRepository: SalesTargetRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        salesTargetRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = SalesTargetService()
        service.salesTargetRepository = salesTargetRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(salesTargetRepository).persist(any<SalesTargetEntity>())
    }

    @Nested
    inner class ListAll {
        @Test
        fun `returns all targets`() {
            val targets =
                listOf(
                    SalesTargetEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        targetMonth = LocalDate.of(2026, 3, 1)
                        targetAmount = 10000000
                    },
                )
            whenever(salesTargetRepository.listByOrganization()).thenReturn(targets)

            val result = service.listAll()

            assertEquals(1, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class FindByStoreAndMonth {
        @Test
        fun `returns target when found`() {
            val storeId = UUID.randomUUID()
            val month = LocalDate.of(2026, 3, 1)
            val entity =
                SalesTargetEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = storeId
                    targetMonth = month
                    targetAmount = 5000000
                }
            whenever(salesTargetRepository.findByStoreAndMonth(storeId, month)).thenReturn(entity)

            val result = service.findByStoreAndMonth(storeId, month)

            assertNotNull(result)
            assertEquals(5000000, result?.targetAmount)
        }
    }

    @Nested
    inner class Upsert {
        @Test
        fun `creates new target when not exists`() {
            val storeId = UUID.randomUUID()
            val month = LocalDate.of(2026, 3, 1)
            whenever(salesTargetRepository.findByStoreAndMonth(storeId, month)).thenReturn(null)

            val result = service.upsert(storeId, month, 10000000)

            assertEquals(orgId, result.organizationId)
            assertEquals(storeId, result.storeId)
            assertEquals(month, result.targetMonth)
            assertEquals(10000000, result.targetAmount)
            verify(salesTargetRepository).persist(any<SalesTargetEntity>())
        }

        @Test
        fun `updates existing target`() {
            val storeId = UUID.randomUUID()
            val month = LocalDate.of(2026, 3, 1)
            val existing =
                SalesTargetEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = storeId
                    targetMonth = month
                    targetAmount = 5000000
                }
            whenever(salesTargetRepository.findByStoreAndMonth(storeId, month)).thenReturn(existing)

            val result = service.upsert(storeId, month, 10000000)

            assertEquals(10000000, result.targetAmount)
            verify(salesTargetRepository).persist(any<SalesTargetEntity>())
        }
    }
}
