package com.openpos.pos.grpc

import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.entity.DrawerEntity
import com.openpos.pos.entity.GiftCardEntity
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.entity.ReservationEntity
import com.openpos.pos.entity.SettlementEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class AuxiliaryProtoMappingTest {
    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000000")

    @Test
    fun `auxiliary entities map to proto messages`() {
        val drawer = drawer().toProto()
        val settlement = settlement().toProto()
        val journal = journalEntry().toProto()
        val giftCard = giftCard().toGiftCardProto()
        val discountReason = discountReason().toDiscountReasonProto()
        val reservation = reservation().toReservationProto()

        assertEquals("33333333-3333-3333-3333-333333333333", drawer.terminalId)
        assertEquals(5000, settlement.difference)
        assertEquals("SALE", journal.type)
        assertEquals("GC-0001-0002-0003", giftCard.code)
        assertTrue(giftCard.expiresAt.isNotBlank())
        assertEquals("MANUAL", discountReason.code)
        assertEquals("山田太郎", reservation.customerName)
        assertEquals("RESERVED", reservation.status)
    }

    @Test
    fun `date strings are parsed as UTC boundaries`() {
        assertEquals(
            Instant.parse("2026-05-07T00:00:00Z"),
            parseInstantOrDate("2026-05-07"),
        )
        assertEquals(
            Instant.parse("2026-05-08T00:00:00Z"),
            parseInstantOrDateExclusive("2026-05-07"),
        )
        assertEquals(
            Instant.parse("2026-05-07T10:15:30Z"),
            parseInstantOrDate("2026-05-07T10:15:30Z"),
        )
    }

    @Test
    fun `gift card without expiry leaves expiresAt unset`() {
        val card = giftCard(expiresAt = null).toGiftCardProto()

        assertTrue(card.expiresAt.isEmpty())
    }

    private fun drawer(): DrawerEntity =
        DrawerEntity().apply {
            id = UUID.fromString("11111111-1111-1111-1111-111111111111")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            storeId = UUID.fromString("22222222-2222-2222-2222-222222222222")
            terminalId = UUID.fromString("33333333-3333-3333-3333-333333333333")
            openingAmount = 10000
            currentAmount = 15000
            isOpen = true
            openedAt = Instant.parse("2026-05-07T00:00:00Z")
            createdAt = Instant.parse("2026-05-07T00:00:00Z")
            updatedAt = Instant.parse("2026-05-07T00:30:00Z")
        }

    private fun settlement(): SettlementEntity =
        SettlementEntity().apply {
            id = UUID.fromString("44444444-4444-4444-4444-444444444444")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            storeId = UUID.fromString("22222222-2222-2222-2222-222222222222")
            terminalId = UUID.fromString("33333333-3333-3333-3333-333333333333")
            staffId = UUID.fromString("55555555-5555-5555-5555-555555555555")
            cashExpected = 100000
            cashActual = 105000
            difference = 5000
            settledAt = Instant.parse("2026-05-07T01:00:00Z")
            createdAt = Instant.parse("2026-05-07T01:00:00Z")
            updatedAt = Instant.parse("2026-05-07T01:05:00Z")
        }

    private fun journalEntry(): JournalEntryEntity =
        JournalEntryEntity().apply {
            id = UUID.fromString("66666666-6666-6666-6666-666666666666")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            type = "SALE"
            transactionId = UUID.fromString("77777777-7777-7777-7777-777777777777")
            staffId = UUID.fromString("55555555-5555-5555-5555-555555555555")
            terminalId = UUID.fromString("33333333-3333-3333-3333-333333333333")
            details = "{\"amount\":10000}"
            createdAt = Instant.parse("2026-05-07T02:00:00Z")
            updatedAt = Instant.parse("2026-05-07T02:00:00Z")
        }

    private fun giftCard(expiresAt: Instant? = Instant.parse("2026-12-31T23:59:59Z")): GiftCardEntity =
        GiftCardEntity().apply {
            id = UUID.fromString("88888888-8888-8888-8888-888888888888")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            code = "GC-0001-0002-0003"
            initialAmount = 50000
            balance = 42000
            status = "ACTIVE"
            issuedAt = Instant.parse("2026-05-07T03:00:00Z")
            this.expiresAt = expiresAt
            createdAt = Instant.parse("2026-05-07T03:00:00Z")
            updatedAt = Instant.parse("2026-05-07T03:00:00Z")
        }

    private fun discountReason(): DiscountReasonEntity =
        DiscountReasonEntity().apply {
            id = UUID.fromString("99999999-9999-9999-9999-999999999999")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            code = "MANUAL"
            description = "手動値引き"
            isActive = true
            createdAt = Instant.parse("2026-05-07T04:00:00Z")
            updatedAt = Instant.parse("2026-05-07T04:00:00Z")
        }

    private fun reservation(): ReservationEntity =
        ReservationEntity().apply {
            id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
            organizationId = this@AuxiliaryProtoMappingTest.organizationId
            storeId = UUID.fromString("22222222-2222-2222-2222-222222222222")
            customerName = "山田太郎"
            customerPhone = "09012345678"
            items = """[{"sku":"P-001","qty":1}]"""
            reservedUntil = Instant.parse("2026-05-08T00:00:00Z")
            status = "RESERVED"
            note = "18時受取"
            createdAt = Instant.parse("2026-05-07T05:00:00Z")
            updatedAt = Instant.parse("2026-05-07T05:00:00Z")
        }
}
