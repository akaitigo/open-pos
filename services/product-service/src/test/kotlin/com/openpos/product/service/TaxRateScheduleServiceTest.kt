package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.entity.TaxRateScheduleEntity
import com.openpos.product.repository.TaxRateRepository
import com.openpos.product.repository.TaxRateScheduleRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

class TaxRateScheduleServiceTest {
    private lateinit var scheduleService: TaxRateScheduleService

    private lateinit var scheduleRepository: TaxRateScheduleRepository

    private lateinit var taxRateRepository: TaxRateRepository

    private lateinit var tenantFilterService: TenantFilterService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        scheduleRepository = mock()
        taxRateRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        scheduleService = TaxRateScheduleService()
        scheduleService.scheduleRepository = scheduleRepository
        scheduleService.taxRateRepository = taxRateRepository
        scheduleService.tenantFilterService = tenantFilterService
        scheduleService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = testOrgId
        doNothing().whenever(scheduleRepository).persist(any<TaxRateScheduleEntity>())
        doNothing().whenever(taxRateRepository).persist(any<TaxRateEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Test
    fun `create should create schedule with correct fields`() {
        val taxRateId = UUID.randomUUID()
        val effectiveDate = LocalDate.of(2027, 4, 1)
        val newRate = BigDecimal("0.1200")

        val schedule = scheduleService.create(taxRateId, newRate, effectiveDate)

        assertNotNull(schedule)
        assertEquals(taxRateId, schedule.taxRateId)
        assertEquals(newRate, schedule.newRate)
        assertEquals(effectiveDate, schedule.effectiveDate)
        assertEquals(false, schedule.applied)
    }

    @Test
    fun `listByTaxRateId should return schedules for given tax rate`() {
        val taxRateId = UUID.randomUUID()
        val schedule1 =
            TaxRateScheduleEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.taxRateId = taxRateId
                newRate = BigDecimal("0.1200")
                effectiveDate = LocalDate.of(2027, 4, 1)
                applied = false
            }
        val schedule2 =
            TaxRateScheduleEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.taxRateId = taxRateId
                newRate = BigDecimal("0.1500")
                effectiveDate = LocalDate.of(2028, 4, 1)
                applied = false
            }

        whenever(scheduleRepository.findByTaxRateId(taxRateId)).thenReturn(listOf(schedule1, schedule2))

        val result = scheduleService.listByTaxRateId(taxRateId)

        assertEquals(2, result.size)
        assertEquals(BigDecimal("0.1200"), result[0].newRate)
        assertEquals(BigDecimal("0.1500"), result[1].newRate)
    }

    @Test
    fun `listByTaxRateId should return empty list when no schedules exist`() {
        val taxRateId = UUID.randomUUID()
        whenever(scheduleRepository.findByTaxRateId(taxRateId)).thenReturn(emptyList())

        val result = scheduleService.listByTaxRateId(taxRateId)

        assertEquals(0, result.size)
    }

    @Test
    fun `applyPendingSchedules should skip when tax rate not found`() {
        val taxRateId = UUID.randomUUID()
        val schedule =
            TaxRateScheduleEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.taxRateId = taxRateId
                newRate = BigDecimal("0.1200")
                effectiveDate = LocalDate.now().minusDays(1)
                applied = false
            }

        whenever(scheduleRepository.findPendingByDate(any())).thenReturn(listOf(schedule))
        val mockQuery1 = mock<PanacheQuery<TaxRateEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(null)
            whenever(taxRateRepository.find(eq("id = ?1"), eq(taxRateId))).thenReturn(mockQuery1)

        val applied = scheduleService.applyPendingSchedules()

        assertEquals(0, applied)
        assertFalse(schedule.applied)
    }

    @Test
    fun `applyPendingSchedules should return 0 when no pending schedules`() {
        whenever(scheduleRepository.findPendingByDate(any())).thenReturn(emptyList())

        val applied = scheduleService.applyPendingSchedules()

        assertEquals(0, applied)
    }

    @Test
    fun `applyPendingSchedules should apply due schedules`() {
        val taxRateId = UUID.randomUUID()
        val taxRate =
            TaxRateEntity().apply {
                id = taxRateId
                organizationId = testOrgId
                name = "標準税率"
                rate = BigDecimal("0.1000")
                taxType = "STANDARD"
            }
        val schedule =
            TaxRateScheduleEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                this.taxRateId = taxRateId
                newRate = BigDecimal("0.1200")
                effectiveDate = LocalDate.now().minusDays(1)
                applied = false
            }

        whenever(scheduleRepository.findPendingByDate(any())).thenReturn(listOf(schedule))
        val mockQuery2 = mock<PanacheQuery<TaxRateEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(taxRate)
            whenever(taxRateRepository.find(eq("id = ?1"), eq(taxRateId))).thenReturn(mockQuery2)

        val applied = scheduleService.applyPendingSchedules()

        assertEquals(1, applied)
        assertTrue(schedule.applied)
        assertEquals(BigDecimal("0.1200"), taxRate.rate)
    }
}
