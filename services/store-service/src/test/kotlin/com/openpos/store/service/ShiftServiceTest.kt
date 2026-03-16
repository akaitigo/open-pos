package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.ShiftEntity
import com.openpos.store.repository.ShiftRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@QuarkusTest
class ShiftServiceTest {
    @Inject
    lateinit var shiftService: ShiftService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var shiftRepository: ShiftRepository

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

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `シフトを正常に作成する`() {
            // Arrange
            doNothing().whenever(shiftRepository).persist(any<ShiftEntity>())
            val date = LocalDate.of(2026, 3, 20)
            val startTime = LocalTime.of(9, 0)
            val endTime = LocalTime.of(17, 0)

            // Act
            val result = shiftService.create(staffId, storeId, date, startTime, endTime, "早番")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(staffId, result.staffId)
            assertEquals(storeId, result.storeId)
            assertEquals(date, result.date)
            assertEquals(startTime, result.startTime)
            assertEquals(endTime, result.endTime)
            assertEquals("早番", result.note)
            verify(shiftRepository).persist(any<ShiftEntity>())
        }

        @Test
        fun `noteがnullでもシフトを作成できる`() {
            // Arrange
            doNothing().whenever(shiftRepository).persist(any<ShiftEntity>())
            val date = LocalDate.of(2026, 3, 21)
            val startTime = LocalTime.of(13, 0)
            val endTime = LocalTime.of(22, 0)

            // Act
            val result = shiftService.create(staffId, storeId, date, startTime, endTime, null)

            // Assert
            assertNotNull(result)
            assertNull(result.note)
            assertEquals(staffId, result.staffId)
        }

        @Test
        fun `organizationIdが未設定の場合はエラー`() {
            // Arrange
            organizationIdHolder.organizationId = null
            val date = LocalDate.of(2026, 3, 20)
            val startTime = LocalTime.of(9, 0)
            val endTime = LocalTime.of(17, 0)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                shiftService.create(staffId, storeId, date, startTime, endTime, null)
            }
        }
    }

    // === listByStoreAndDate ===

    @Nested
    inner class ListByStoreAndDate {
        @Test
        fun `店舗と日付でシフト一覧を取得する`() {
            // Arrange
            val date = LocalDate.of(2026, 3, 20)
            val shift1 = createShiftEntity(staffId, date, LocalTime.of(9, 0), LocalTime.of(17, 0), "早番")
            val staffId2 = UUID.randomUUID()
            val shift2 = createShiftEntity(staffId2, date, LocalTime.of(17, 0), LocalTime.of(22, 0), "遅番")
            whenever(shiftRepository.listByStoreAndDate(storeId, date))
                .thenReturn(listOf(shift1, shift2))

            // Act
            val result = shiftService.listByStoreAndDate(storeId, date)

            // Assert
            assertEquals(2, result.size)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `シフトが存在しない場合は空リストを返す`() {
            // Arrange
            val date = LocalDate.of(2026, 3, 25)
            whenever(shiftRepository.listByStoreAndDate(storeId, date))
                .thenReturn(emptyList())

            // Act
            val result = shiftService.listByStoreAndDate(storeId, date)

            // Assert
            assertEquals(0, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === delete ===

    @Nested
    inner class Delete {
        @Test
        fun `シフトを正常に削除する`() {
            // Arrange
            val shiftId = UUID.randomUUID()
            val entity = createShiftEntity(staffId, LocalDate.of(2026, 3, 20), LocalTime.of(9, 0), LocalTime.of(17, 0), null)
            entity.id = shiftId
            whenever(shiftRepository.findById(shiftId)).thenReturn(entity)
            doNothing().whenever(shiftRepository).delete(any<ShiftEntity>())

            // Act
            val result = shiftService.delete(shiftId)

            // Assert
            assertTrue(result)
            verify(tenantFilterService).enableFilter()
            verify(shiftRepository).delete(entity)
        }

        @Test
        fun `存在しないシフトIDの場合はfalseを返す`() {
            // Arrange
            val shiftId = UUID.randomUUID()
            whenever(shiftRepository.findById(shiftId)).thenReturn(null)

            // Act
            val result = shiftService.delete(shiftId)

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === helper ===

    private fun createShiftEntity(
        staffId: UUID,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        note: String?,
    ): ShiftEntity =
        ShiftEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.staffId = staffId
            this.storeId = this@ShiftServiceTest.storeId
            this.date = date
            this.startTime = startTime
            this.endTime = endTime
            this.note = note
        }
}
