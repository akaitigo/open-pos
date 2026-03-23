package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.ShiftEntity
import com.openpos.store.repository.ShiftRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class ShiftServiceTest {
    private lateinit var service: ShiftService
    private lateinit var shiftRepository: ShiftRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        shiftRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = ShiftService()
        service.shiftRepository = shiftRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class Create {
        @Test
        fun `creates shift with correct fields`() {
            doNothing().whenever(shiftRepository).persist(any<ShiftEntity>())

            val date = LocalDate.of(2026, 3, 24)
            val startTime = LocalTime.of(9, 0)
            val endTime = LocalTime.of(17, 0)
            val result = service.create(staffId, storeId, date, startTime, endTime, "Morning shift")

            assertEquals(orgId, result.organizationId)
            assertEquals(staffId, result.staffId)
            assertEquals(storeId, result.storeId)
            assertEquals(date, result.date)
            assertEquals(startTime, result.startTime)
            assertEquals(endTime, result.endTime)
            assertEquals("Morning shift", result.note)
            verify(shiftRepository).persist(any<ShiftEntity>())
        }

        @Test
        fun `creates shift with null note`() {
            doNothing().whenever(shiftRepository).persist(any<ShiftEntity>())

            val result = service.create(staffId, storeId, LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), null)

            assertEquals(null, result.note)
        }
    }

    @Nested
    inner class ListByStoreAndDate {
        @Test
        fun `returns shifts for store and date`() {
            val date = LocalDate.of(2026, 3, 24)
            val shifts =
                listOf(
                    ShiftEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.staffId = this@ShiftServiceTest.staffId
                        this.storeId = this@ShiftServiceTest.storeId
                        this.date = date
                        this.startTime = LocalTime.of(9, 0)
                        this.endTime = LocalTime.of(17, 0)
                    },
                )
            whenever(shiftRepository.listByStoreAndDate(storeId, date)).thenReturn(shifts)

            val result = service.listByStoreAndDate(storeId, date)

            assertEquals(1, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deletes shift and returns true`() {
            val shiftId = UUID.randomUUID()
            val entity =
                ShiftEntity().apply {
                    this.id = shiftId
                    this.organizationId = orgId
                    this.staffId = this@ShiftServiceTest.staffId
                    this.storeId = this@ShiftServiceTest.storeId
                    this.date = LocalDate.now()
                    this.startTime = LocalTime.of(9, 0)
                    this.endTime = LocalTime.of(17, 0)
                }
            whenever(shiftRepository.findById(shiftId)).thenReturn(entity)
            doNothing().whenever(shiftRepository).delete(any<ShiftEntity>())

            val result = service.delete(shiftId)

            assertTrue(result)
            verify(shiftRepository).delete(any<ShiftEntity>())
        }

        @Test
        fun `returns false when shift not found`() {
            val shiftId = UUID.randomUUID()
            whenever(shiftRepository.findById(shiftId)).thenReturn(null)

            val result = service.delete(shiftId)

            assertFalse(result)
        }
    }
}
