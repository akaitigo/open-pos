package com.openpos.pos.grpc

import com.openpos.pos.entity.ReservationEntity
import openpos.pos.v1.CreateReservationRequest
import openpos.pos.v1.ListReservationsRequest
import java.time.Instant
import java.util.UUID

internal data class ReservationListQuery(
    val storeId: UUID,
    val status: String?,
)

internal data class ReservationCreateInput(
    val storeId: UUID,
    val customerName: String?,
    val customerPhone: String?,
    val items: String,
    val reservedUntil: Instant,
    val note: String?,
)

internal fun resolveReservationListQuery(request: ListReservationsRequest): ReservationListQuery =
    ReservationListQuery(
        storeId = request.storeId.toUUID(),
        status = request.status.ifBlank { null },
    )

internal fun resolveReservationCreateInput(request: CreateReservationRequest): ReservationCreateInput =
    ReservationCreateInput(
        storeId = request.storeId.toUUID(),
        customerName = request.customerName.ifBlank { null },
        customerPhone = request.customerPhone.ifBlank { null },
        items = request.items,
        reservedUntil = Instant.parse(request.reservedUntil),
        note = request.note.ifBlank { null },
    )

internal fun requireReservation(
    reservationId: UUID,
    loadReservation: (UUID) -> ReservationEntity?,
): ReservationEntity =
    loadReservation(reservationId)
        ?: throw ResourceNotFoundException("Reservation not found: $reservationId")
