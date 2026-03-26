package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.ReservationEntity
import com.openpos.pos.repository.ReservationRepository
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * ReservationService の純粋ユニットテスト。
 * CDI プロキシを回避してカバレッジを確保する。
 */
class ReservationServiceUnitTest {
    private lateinit var service: ReservationService
    private lateinit var reservationRepository: ReservationRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        reservationRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = ReservationService()
        service.reservationRepository = reservationRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class FindById {
        @Test
        fun `returns reservation when found`() {
            val id = UUID.randomUUID()
            val entity =
                ReservationEntity().apply {
                    this.id = id
                    this.organizationId = orgId
                    this.storeId = this@ReservationServiceUnitTest.storeId
                    this.customerName = "Test"
                    this.items = "[]"
                    this.reservedUntil = Instant.now().plusSeconds(3600)
                }
            whenever(reservationRepository.findById(id)).thenReturn(entity)

            val result = service.findById(id)

            assertNotNull(result)
            assertEquals(id, result?.id)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            whenever(reservationRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.findById(UUID.randomUUID())

            assertNull(result)
        }
    }

    @Nested
    inner class ListByStoreId {
        @Test
        fun `returns all reservations when no status filter`() {
            val items = listOf(createReservation())
            whenever(reservationRepository.listByStoreId(eq(storeId), any<Page>())).thenReturn(items)
            whenever(reservationRepository.countByStoreId(storeId)).thenReturn(1L)

            val (result, total) = service.listByStoreId(storeId, null, 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
        }

        @Test
        fun `filters by status when provided`() {
            val items = listOf(createReservation())
            whenever(reservationRepository.listByStoreIdAndStatus(eq(storeId), eq("RESERVED"), any<Page>())).thenReturn(items)
            whenever(reservationRepository.countByStoreIdAndStatus(storeId, "RESERVED")).thenReturn(1L)

            val (result, total) = service.listByStoreId(storeId, "RESERVED", 0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `creates reservation with all fields`() {
            doNothing().whenever(reservationRepository).persist(any<ReservationEntity>())
            val reservedUntil = Instant.now().plusSeconds(7200)

            val result = service.create(storeId, "Customer", "090-1234-5678", "[{\"productId\":\"abc\"}]", reservedUntil, "Note")

            assertEquals(orgId, result.organizationId)
            assertEquals(storeId, result.storeId)
            assertEquals("Customer", result.customerName)
            assertEquals("090-1234-5678", result.customerPhone)
            assertEquals("[{\"productId\":\"abc\"}]", result.items)
            assertEquals(reservedUntil, result.reservedUntil)
            assertEquals("Note", result.note)
        }

        @Test
        fun `throws when organizationId is not set`() {
            organizationIdHolder.organizationId = null

            assertThrows(IllegalArgumentException::class.java) {
                service.create(storeId, "Customer", null, "[]", Instant.now().plusSeconds(3600), null)
            }
        }
    }

    @Nested
    inner class Fulfill {
        @Test
        fun `marks reservation as FULFILLED`() {
            val id = UUID.randomUUID()
            val entity =
                createReservation().apply {
                    this.id = id
                    this.status = "RESERVED"
                }
            whenever(reservationRepository.findById(id)).thenReturn(entity)
            doNothing().whenever(reservationRepository).persist(any<ReservationEntity>())

            val result = service.fulfill(id)

            assertNotNull(result)
            assertEquals("FULFILLED", result?.status)
        }

        @Test
        fun `returns null when not found`() {
            whenever(reservationRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.fulfill(UUID.randomUUID())

            assertNull(result)
        }
    }

    @Nested
    inner class Cancel {
        @Test
        fun `marks reservation as CANCELLED`() {
            val id = UUID.randomUUID()
            val entity =
                createReservation().apply {
                    this.id = id
                    this.status = "RESERVED"
                }
            whenever(reservationRepository.findById(id)).thenReturn(entity)
            doNothing().whenever(reservationRepository).persist(any<ReservationEntity>())

            val result = service.cancel(id)

            assertNotNull(result)
            assertEquals("CANCELLED", result?.status)
        }

        @Test
        fun `returns null when not found`() {
            whenever(reservationRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.cancel(UUID.randomUUID())

            assertNull(result)
        }
    }

    private fun createReservation(): ReservationEntity =
        ReservationEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@ReservationServiceUnitTest.storeId
            this.customerName = "Test Customer"
            this.items = "[]"
            this.reservedUntil = Instant.now().plusSeconds(3600)
            this.status = "RESERVED"
        }
}
