package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.AttendanceEntity
import com.openpos.store.repository.AttendanceRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
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

class AttendanceServiceTest {
    private lateinit var service: AttendanceService
    private lateinit var attendanceRepository: AttendanceRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        attendanceRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = AttendanceService()
        service.attendanceRepository = attendanceRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class ClockIn {
        @Test
        fun `creates attendance record with clockIn time`() {
            doNothing().whenever(attendanceRepository).persist(any<AttendanceEntity>())

            val result = service.clockIn(staffId, storeId)

            assertEquals(orgId, result.organizationId)
            assertEquals(staffId, result.staffId)
            assertEquals(storeId, result.storeId)
            assertNotNull(result.clockIn)
            assertEquals(LocalDate.now(), result.date)
            verify(attendanceRepository).persist(any<AttendanceEntity>())
        }
    }

    @Nested
    inner class ClockOut {
        @Test
        fun `sets clockOut on existing attendance`() {
            val entity =
                AttendanceEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.staffId = this@AttendanceServiceTest.staffId
                    this.storeId = this@AttendanceServiceTest.storeId
                    this.date = LocalDate.now()
                    this.clockIn =
                        java.time.Instant
                            .now()
                            .minusSeconds(3600)
                }
            whenever(attendanceRepository.findByStaffAndDate(staffId, LocalDate.now())).thenReturn(entity)
            doNothing().whenever(attendanceRepository).persist(any<AttendanceEntity>())

            val result = service.clockOut(staffId)

            assertNotNull(result)
            assertNotNull(result?.clockOut)
            verify(tenantFilterService).enableFilter()
            verify(attendanceRepository).persist(any<AttendanceEntity>())
        }

        @Test
        fun `returns null when no attendance found`() {
            whenever(attendanceRepository.findByStaffAndDate(staffId, LocalDate.now())).thenReturn(null)

            val result = service.clockOut(staffId)

            assertNull(result)
        }
    }

    @Nested
    inner class ListByStoreAndDate {
        @Test
        fun `returns attendance list for store and date`() {
            val date = LocalDate.of(2026, 3, 23)
            val records =
                listOf(
                    AttendanceEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.staffId = this@AttendanceServiceTest.staffId
                        this.storeId = this@AttendanceServiceTest.storeId
                        this.date = date
                        this.clockIn = java.time.Instant.now()
                    },
                )
            whenever(attendanceRepository.listByStoreAndDate(storeId, date)).thenReturn(records)

            val result = service.listByStoreAndDate(storeId, date)

            assertEquals(1, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }
}
