package com.openpos.pos.grpc

import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.entity.DrawerEntity
import com.openpos.pos.entity.GiftCardEntity
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.entity.ReservationEntity
import com.openpos.pos.entity.SettlementEntity
import openpos.pos.v1.DiscountReason
import openpos.pos.v1.Drawer
import openpos.pos.v1.GiftCard
import openpos.pos.v1.JournalEntry
import openpos.pos.v1.Reservation
import openpos.pos.v1.Settlement
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

internal fun DrawerEntity.toProto(): Drawer =
    Drawer
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setStoreId(storeId.toString())
        .setTerminalId(terminalId.toString())
        .setOpeningAmount(openingAmount)
        .setCurrentAmount(currentAmount)
        .setIsOpen(isOpen)
        .setOpenedAt(openedAt?.toString().orEmpty())
        .setClosedAt(closedAt?.toString().orEmpty())
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()

internal fun SettlementEntity.toProto(): Settlement =
    Settlement
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setStoreId(storeId.toString())
        .setTerminalId(terminalId.toString())
        .setStaffId(staffId.toString())
        .setCashExpected(cashExpected)
        .setCashActual(cashActual)
        .setDifference(difference)
        .setSettledAt(settledAt.toString())
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()

internal fun JournalEntryEntity.toProto(): JournalEntry =
    JournalEntry
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setType(type)
        .setTransactionId(transactionId?.toString().orEmpty())
        .setStaffId(staffId.toString())
        .setTerminalId(terminalId.toString())
        .setDetails(details)
        .setCreatedAt(createdAt.toString())
        .build()

internal fun DiscountReasonEntity.toDiscountReasonProto(): DiscountReason =
    DiscountReason
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setCode(code)
        .setDescription(description)
        .setIsActive(isActive)
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()

internal fun ReservationEntity.toReservationProto(): Reservation =
    Reservation
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setStoreId(storeId.toString())
        .setCustomerName(customerName.orEmpty())
        .setCustomerPhone(customerPhone.orEmpty())
        .setItems(items)
        .setReservedUntil(reservedUntil.toString())
        .setStatus(status)
        .setNote(note.orEmpty())
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()

internal fun GiftCardEntity.toGiftCardProto(): GiftCard =
    GiftCard
        .newBuilder()
        .setId(id.toString())
        .setOrganizationId(organizationId.toString())
        .setCode(code)
        .setInitialAmount(initialAmount)
        .setBalance(balance)
        .setStatus(status)
        .setIssuedAt(issuedAt.toString())
        .apply { this@toGiftCardProto.expiresAt?.let { setExpiresAt(it.toString()) } }
        .setCreatedAt(createdAt.toString())
        .setUpdatedAt(updatedAt.toString())
        .build()

/**
 * Instant 形式（ISO-8601）または日付文字列（YYYY-MM-DD）をパースして Instant を返す。
 * 日付文字列の場合は UTC の開始時刻（00:00:00Z）に変換する。
 */
internal fun parseInstantOrDate(value: String): Instant =
    try {
        Instant.parse(value)
    } catch (_: java.time.format.DateTimeParseException) {
        LocalDate.parse(value).atStartOfDay(ZoneOffset.UTC).toInstant()
    }

/**
 * endDate 用のパース関数。
 * 日付文字列（YYYY-MM-DD）の場合は翌日の開始時刻に変換し、排他的上限として使用する。
 */
internal fun parseInstantOrDateExclusive(value: String): Instant =
    try {
        Instant.parse(value)
    } catch (_: java.time.format.DateTimeParseException) {
        LocalDate
            .parse(value)
            .plusDays(1)
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
    }
