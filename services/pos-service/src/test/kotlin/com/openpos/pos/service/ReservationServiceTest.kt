package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.ReservationEntity
import com.openpos.pos.repository.ReservationRepository
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
import java.time.Instant
import java.util.UUID

@QuarkusTest
class ReservationServiceTest {
    @Inject
    lateinit var reservationService: ReservationService

    @InjectMock
    lateinit var reservationRepository: ReservationRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()
    private val testStoreId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(reservationRepository).persist(any<ReservationEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        whenever(organizationIdHolder.organizationId).thenReturn(testOrgId)
    }

    @Test
    fun `create should create reservation with RESERVED status`() {
        val reservation =
            reservationService.create(
                storeId = testStoreId,
                customerName = "テスト太郎",
                customerPhone = "090-1234-5678",
                items = """[{"productId":"abc","quantity":2}]""",
                reservedUntil = Instant.now().plusSeconds(86400),
                note = "取り置き依頼",
            )

        assertNotNull(reservation)
        assertEquals("テスト太郎", reservation.customerName)
        assertEquals("RESERVED", reservation.status)
        assertEquals(testOrgId, reservation.organizationId)
    }

    @Test
    fun `fulfill should change status to FULFILLED`() {
        val reservationId = UUID.randomUUID()
        val entity =
            ReservationEntity().apply {
                id = reservationId
                organizationId = testOrgId
                storeId = testStoreId
                reservedUntil = Instant.now().plusSeconds(86400)
                status = "RESERVED"
            }
        whenever(reservationRepository.findById(reservationId)).thenReturn(entity)

        val result = reservationService.fulfill(reservationId)

        assertNotNull(result)
        assertEquals("FULFILLED", result?.status)
    }

    @Test
    fun `cancel should change status to CANCELLED`() {
        val reservationId = UUID.randomUUID()
        val entity =
            ReservationEntity().apply {
                id = reservationId
                organizationId = testOrgId
                storeId = testStoreId
                reservedUntil = Instant.now().plusSeconds(86400)
                status = "RESERVED"
            }
        whenever(reservationRepository.findById(reservationId)).thenReturn(entity)

        val result = reservationService.cancel(reservationId)

        assertNotNull(result)
        assertEquals("CANCELLED", result?.status)
    }
}
