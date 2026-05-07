package com.openpos.pos.grpc

import com.openpos.pos.entity.ReservationEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import openpos.pos.v1.CreateReservationRequest
import openpos.pos.v1.ListReservationsRequest
import java.time.Instant
import java.util.UUID

class ReservationSupportTest {
    private val storeId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val reservationId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `resolves reservation list query with optional status`() {
        val withStatus =
            resolveReservationListQuery(
                ListReservationsRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setStatus("PENDING")
                    .build(),
            )
        val withoutStatus =
            resolveReservationListQuery(
                ListReservationsRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setStatus("")
                    .build(),
            )

        assertEquals(storeId, withStatus.storeId)
        assertEquals("PENDING", withStatus.status)
        assertEquals(storeId, withoutStatus.storeId)
        assertNull(withoutStatus.status)
    }

    @Test
    fun `resolves reservation create input and normalizes blanks`() {
        val input =
            resolveReservationCreateInput(
                CreateReservationRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setCustomerName("")
                    .setCustomerPhone("")
                    .setItems("{\"sku\":\"coffee\"}")
                    .setReservedUntil("2026-05-10T09:00:00Z")
                    .setNote("")
                    .build(),
            )

        assertEquals(storeId, input.storeId)
        assertNull(input.customerName)
        assertNull(input.customerPhone)
        assertEquals("{\"sku\":\"coffee\"}", input.items)
        assertEquals(Instant.parse("2026-05-10T09:00:00Z"), input.reservedUntil)
        assertNull(input.note)
    }

    @Test
    fun `requires reservation to exist`() {
        val reservation = ReservationEntity().apply { id = reservationId }

        val resolved = requireReservation(reservationId) { reservation }

        assertSame(reservation, resolved)
    }

    @Test
    fun `raises resource not found when reservation is absent`() {
        val error =
            assertThrows(ResourceNotFoundException::class.java) {
                requireReservation(reservationId) { null }
            }

        assertEquals("Reservation not found: $reservationId", error.message)
    }
}
