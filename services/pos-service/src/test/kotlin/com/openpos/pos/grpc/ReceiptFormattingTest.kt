package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ReceiptFormattingTest {
    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val transactionId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    fun `buildReceiptData includes change and reduced tax note`() {
        val receiptData = buildReceiptData(transaction(), items(), payments(), taxSummaries())

        assertTrue(receiptData.contains("取引番号: T-2026-000001"))
        assertTrue(receiptData.contains("お釣り: 50円"))
        assertTrue(receiptData.contains("※軽減税率8%: 対象150円 税12円"))
        assertTrue(receiptData.contains("※は軽減税率対象商品"))
        assertFalse(receiptData.contains("割引:"))
    }

    @Test
    fun `buildInvoiceReceiptData includes invoice number and inclusive breakdown`() {
        val invoiceReceipt =
            buildInvoiceReceiptData(
                tx = transaction(discountTotal = 1000, total = 32500),
                items = items(),
                payments = payments(),
                taxSummaries = taxSummaries(),
                invoiceNumber = "T1234567890123",
            )

        assertTrue(invoiceReceipt.contains("（適格簡易請求書）"))
        assertTrue(invoiceReceipt.contains("登録番号: T1234567890123"))
        assertTrue(invoiceReceipt.contains("割引:    -10円"))
        assertTrue(invoiceReceipt.contains("【税率別内訳（税込）】"))
        assertTrue(invoiceReceipt.contains("※は軽減税率(8%)対象商品"))
        assertTrue(invoiceReceipt.contains("上記正に領収いたしました"))
    }

    @Test
    fun `buildReceiptProto populates receipt metadata and data`() {
        val createdAt = Instant.parse("2026-05-07T00:45:00Z")
        val receipt =
            buildReceiptProto(
                tx = transaction(),
                items = items(),
                payments = payments(),
                taxSummaries = taxSummaries(),
                now = createdAt,
                receiptId = "receipt-123",
            )

        assertEquals("receipt-123", receipt.id)
        assertEquals(transactionId.toString(), receipt.transactionId)
        assertEquals(createdAt.toString(), receipt.createdAt)
        assertTrue(receipt.receiptData.contains("テスト商品A"))
    }

    private fun transaction(
        discountTotal: Long = 0,
        total: Long = 33500,
    ): TransactionEntity =
        TransactionEntity().apply {
            id = transactionId
            organizationId = this@ReceiptFormattingTest.organizationId
            storeId = UUID.fromString("22222222-2222-2222-2222-222222222222")
            terminalId = UUID.fromString("33333333-3333-3333-3333-333333333333")
            staffId = UUID.fromString("44444444-4444-4444-4444-444444444444")
            transactionNumber = "T-2026-000001"
            status = "COMPLETED"
            subtotal = 30000
            taxTotal = 3500
            this.discountTotal = discountTotal
            this.total = total
            completedAt = Instant.parse("2026-05-07T00:30:00Z")
            createdAt = Instant.parse("2026-05-07T00:20:00Z")
            updatedAt = Instant.parse("2026-05-07T00:30:00Z")
        }

    private fun items(): List<TransactionItemEntity> =
        listOf(
            TransactionItemEntity().apply {
                id = UUID.fromString("55555555-5555-5555-5555-555555555555")
                organizationId = this@ReceiptFormattingTest.organizationId
                transactionId = this@ReceiptFormattingTest.transactionId
                productId = UUID.fromString("66666666-6666-6666-6666-666666666666")
                productName = "テスト商品A"
                unitPrice = 15000
                quantity = 1
                taxRateName = "標準税率10%"
                taxRate = "0.10"
                subtotal = 15000
                taxAmount = 1500
                total = 16500
            },
            TransactionItemEntity().apply {
                id = UUID.fromString("77777777-7777-7777-7777-777777777777")
                organizationId = this@ReceiptFormattingTest.organizationId
                transactionId = this@ReceiptFormattingTest.transactionId
                productId = UUID.fromString("88888888-8888-8888-8888-888888888888")
                productName = "軽減商品B"
                unitPrice = 15000
                quantity = 1
                taxRateName = "軽減税率8%"
                taxRate = "0.08"
                isReducedTax = true
                subtotal = 15000
                taxAmount = 1200
                total = 16200
            },
        )

    private fun payments(): List<PaymentEntity> =
        listOf(
            PaymentEntity().apply {
                id = UUID.fromString("99999999-9999-9999-9999-999999999999")
                organizationId = this@ReceiptFormattingTest.organizationId
                transactionId = this@ReceiptFormattingTest.transactionId
                method = "CASH"
                amount = 33500
                received = 38500
                change = 5000
            },
        )

    private fun taxSummaries(): List<TaxSummaryEntity> =
        listOf(
            TaxSummaryEntity().apply {
                id = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa")
                organizationId = this@ReceiptFormattingTest.organizationId
                transactionId = this@ReceiptFormattingTest.transactionId
                taxRateName = "標準税率10%"
                taxRate = "0.10"
                taxableAmount = 15000
                taxAmount = 1500
            },
            TaxSummaryEntity().apply {
                id = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb")
                organizationId = this@ReceiptFormattingTest.organizationId
                transactionId = this@ReceiptFormattingTest.transactionId
                taxRateName = "軽減税率8%"
                taxRate = "0.08"
                isReduced = true
                taxableAmount = 15000
                taxAmount = 1200
            },
        )
}
