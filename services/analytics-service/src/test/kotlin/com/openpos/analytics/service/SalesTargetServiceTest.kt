package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.SalesTargetEntity
import com.openpos.analytics.repository.SalesTargetRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class SalesTargetServiceTest {
    @Inject
    lateinit var salesTargetService: SalesTargetService

    @InjectMock
    lateinit var salesTargetRepository: SalesTargetRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(salesTargetRepository).persist(any<SalesTargetEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        whenever(organizationIdHolder.organizationId).thenReturn(testOrgId)
    }

    @Test
    fun `upsert should create new target when not exists`() {
        val storeId = UUID.randomUUID()
        val targetMonth = LocalDate.of(2026, 3, 1)
        whenever(salesTargetRepository.findByStoreAndMonth(storeId, targetMonth)).thenReturn(null)

        val result = salesTargetService.upsert(storeId, targetMonth, 10000000)

        assertNotNull(result)
        assertEquals(storeId, result.storeId)
        assertEquals(targetMonth, result.targetMonth)
        assertEquals(10000000, result.targetAmount)
    }

    @Test
    fun `upsert should update existing target`() {
        val storeId = UUID.randomUUID()
        val targetMonth = LocalDate.of(2026, 3, 1)
        val existing =
            SalesTargetEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.storeId = storeId
                this.targetMonth = targetMonth
                targetAmount = 5000000
            }
        whenever(salesTargetRepository.findByStoreAndMonth(storeId, targetMonth)).thenReturn(existing)

        val result = salesTargetService.upsert(storeId, targetMonth, 10000000)

        assertNotNull(result)
        assertEquals(10000000, result.targetAmount)
    }
}
