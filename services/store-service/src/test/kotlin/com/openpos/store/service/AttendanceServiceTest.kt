package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.AttendanceEntity
import com.openpos.store.repository.AttendanceRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class AttendanceServiceTest {
    @Inject
    lateinit var attendanceService: AttendanceService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var attendanceRepository: AttendanceRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === clockIn ===

    @Nested
    inner class ClockIn {
        @Test
        fun `出勤打刻を正常に記録する`() {
            // Arrange
            doNothing().whenever(attendanceRepository).persist(any<AttendanceEntity>())

            // Act
            val result = attendanceService.clockIn(staffId, storeId)

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(staffId, result.staffId)
            assertEquals(storeId, result.storeId)
            assertEquals(LocalDate.now(), result.date)
            assertNotNull(result.clockIn)
            assertNull(result.clockOut)
            verify(attendanceRepository).persist(any<AttendanceEntity>())
        }

        @Test
        fun `organizationIdが未設定の場合はエラー`() {
            // Arrange
            organizationIdHolder.organizationId = null

            // Act & Assert
            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException::class.java) {
                attendanceService.clockIn(staffId, storeId)
            }
        }
    }

    // === clockOut ===

    @Nested
    inner class ClockOut {
        @Test
        fun `退勤打刻を正常に記録する`() {
            // Arrange
            val entity = createAttendanceEntity(staffId, storeId)
            whenever(attendanceRepository.findByStaffAndDate(any(), any())).thenReturn(entity)
            doNothing().whenever(attendanceRepository).persist(any<AttendanceEntity>())

            // Act
            val result = attendanceService.clockOut(staffId)

            // Assert
            assertNotNull(result)
            assertNotNull(result?.clockOut)
            verify(tenantFilterService).enableFilter()
            verify(attendanceRepository).persist(any<AttendanceEntity>())
        }

        @Test
        fun `出勤記録が存在しない場合はnullを返す`() {
            // Arrange
            whenever(attendanceRepository.findByStaffAndDate(any(), any())).thenReturn(null)

            // Act
            val result = attendanceService.clockOut(staffId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === listByStoreAndDate ===

    @Nested
    inner class ListByStoreAndDate {
        @Test
        fun `店舗と日付で勤怠一覧を取得する`() {
            // Arrange
            val today = LocalDate.now()
            val entity1 = createAttendanceEntity(staffId, storeId)
            val staffId2 = UUID.randomUUID()
            val entity2 = createAttendanceEntity(staffId2, storeId)
            whenever(attendanceRepository.listByStoreAndDate(storeId, today))
                .thenReturn(listOf(entity1, entity2))

            // Act
            val result = attendanceService.listByStoreAndDate(storeId, today)

            // Assert
            assertEquals(2, result.size)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `勤怠記録が存在しない場合は空リストを返す`() {
            // Arrange
            val today = LocalDate.now()
            whenever(attendanceRepository.listByStoreAndDate(storeId, today))
                .thenReturn(emptyList())

            // Act
            val result = attendanceService.listByStoreAndDate(storeId, today)

            // Assert
            assertEquals(0, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === helper ===

    private fun createAttendanceEntity(
        staffId: UUID,
        storeId: UUID,
    ): AttendanceEntity =
        AttendanceEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.staffId = staffId
            this.storeId = storeId
            this.date = LocalDate.now()
            this.clockIn = Instant.now()
        }
}
