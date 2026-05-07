package com.openpos.pos.grpc

import com.openpos.pos.entity.TransactionEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import openpos.pos.v1.OfflineTransaction
import openpos.pos.v1.OfflineTransactionItem
import openpos.pos.v1.PaymentInput
import openpos.pos.v1.PaymentMethod
import java.time.Instant
import java.util.UUID

class OfflineSyncSupportTest {
    private val storeId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val terminalId = UUID.fromString("22222222-2222-2222-2222-222222222222")
    private val staffId = UUID.fromString("33333333-3333-3333-3333-333333333333")
    private val transactionId = UUID.fromString("44444444-4444-4444-4444-444444444444")

    @Test
    fun `maps offline transaction payload into sync input and success result`() {
        var capturedInput: OfflineTransactionSyncInput? = null

        val result =
            syncOfflineTransactionResult(
                offlineTransaction =
                    OfflineTransaction
                        .newBuilder()
                        .setClientId("offline-1")
                        .setStoreId(storeId.toString())
                        .setTerminalId(terminalId.toString())
                        .setStaffId(staffId.toString())
                        .addItems(
                            OfflineTransactionItem
                                .newBuilder()
                                .setProductId(UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa").toString())
                                .setProductName("Coffee")
                                .setUnitPrice(480)
                                .setQuantity(2)
                                .setTaxRateName("standard")
                                .setTaxRate("0.10")
                                .setIsReducedTax(false)
                                .build(),
                        ).addPayments(
                            PaymentInput
                                .newBuilder()
                                .setMethod(PaymentMethod.PAYMENT_METHOD_CASH)
                                .setAmount(960)
                                .setReceived(1000)
                                .setReference("")
                                .build(),
                        ).setCreatedAt("2026-05-07T00:00:00Z")
                        .build(),
            ) { input ->
                capturedInput = input
                TransactionEntity().apply { id = transactionId }
            }

        assertTrue(result.success)
        assertEquals(transactionId.toString(), result.transactionId)
        assertEquals("offline-1", capturedInput?.clientId)
        assertEquals(storeId, capturedInput?.storeId)
        assertEquals(terminalId, capturedInput?.terminalId)
        assertEquals(staffId, capturedInput?.staffId)
        assertEquals(1, capturedInput?.items?.size)
        assertEquals("Coffee", capturedInput?.items?.single()?.productName)
        assertEquals("CASH", capturedInput?.payments?.single()?.method)
        assertEquals(1000, capturedInput?.payments?.single()?.received)
        assertEquals(Instant.parse("2026-05-07T00:00:00Z"), capturedInput?.createdAt)
    }

    @Test
    fun `blank optional fields become nulls`() {
        var capturedInput: OfflineTransactionSyncInput? = null

        syncOfflineTransactionResult(
            offlineTransaction =
                OfflineTransaction
                    .newBuilder()
                    .setClientId("offline-2")
                    .setStoreId(storeId.toString())
                    .setTerminalId(terminalId.toString())
                    .setStaffId(staffId.toString())
                    .addPayments(
                        PaymentInput
                            .newBuilder()
                            .setMethod(PaymentMethod.PAYMENT_METHOD_CREDIT_CARD)
                            .setAmount(1200)
                            .build(),
                    ).build(),
        ) { input ->
            capturedInput = input
            TransactionEntity().apply { id = transactionId }
        }

        assertNull(capturedInput?.createdAt)
        assertNull(capturedInput?.payments?.single()?.received)
        assertNull(capturedInput?.payments?.single()?.reference)
    }

    @Test
    fun `returns failure result when sync raises`() {
        val result =
            syncOfflineTransactionResult(
                offlineTransaction =
                    OfflineTransaction
                        .newBuilder()
                        .setClientId("offline-3")
                        .setStoreId(storeId.toString())
                        .setTerminalId(terminalId.toString())
                        .setStaffId(staffId.toString())
                        .build(),
            ) {
                throw IllegalArgumentException("invalid state")
            }

        assertFalse(result.success)
        assertEquals("offline-3", result.clientId)
        assertEquals("invalid state", result.error)
    }
}
