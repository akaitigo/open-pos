package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

class ReceiptAccessSupportTest {
    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val transactionId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    fun `loads receipt transaction view for completed transaction`() {
        val items = listOf(TransactionItemEntity())
        val payments = listOf(PaymentEntity())
        val taxSummaries = listOf(TaxSummaryEntity())

        val view =
            loadReceiptTransactionView(
                transactionId = transactionId,
                loadTransaction = { transaction(status = "COMPLETED") },
                loadItems = {
                    assertEquals(transactionId, it)
                    items
                },
                loadPayments = { payments },
                loadTaxSummaries = { taxSummaries },
            )

        assertEquals("COMPLETED", view.transaction.status)
        assertSame(items, view.items)
        assertSame(payments, view.payments)
        assertSame(taxSummaries, view.taxSummaries)
    }

    @Test
    fun `rejects receipt access for draft transaction`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                loadReceiptTransactionView(
                    transactionId = transactionId,
                    loadTransaction = { transaction(status = "DRAFT") },
                    loadItems = { emptyList() },
                    loadPayments = { emptyList() },
                    loadTaxSummaries = { emptyList() },
                )
            }

        assertEquals("Receipt is only available for COMPLETED or VOIDED transactions", error.message)
    }

    @Test
    fun `loads invoice receipt view and resolves invoice number`() {
        val invoiceView =
            loadInvoiceReceiptView(
                transactionId = transactionId,
                loadTransaction = { transaction(status = "COMPLETED") },
                loadItems = { emptyList() },
                loadPayments = { emptyList() },
                loadTaxSummaries = { emptyList() },
                getInvoiceNumber = { orgId ->
                    assertEquals(organizationId, orgId)
                    "T1234567890123"
                },
            )

        assertEquals("T1234567890123", invoiceView.invoiceNumber)
        assertEquals("COMPLETED", invoiceView.receipt.transaction.status)
    }

    @Test
    fun `rejects invoice receipt access for non completed transaction`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                loadInvoiceReceiptView(
                    transactionId = transactionId,
                    loadTransaction = { transaction(status = "VOIDED") },
                    loadItems = { emptyList() },
                    loadPayments = { emptyList() },
                    loadTaxSummaries = { emptyList() },
                    getInvoiceNumber = { "T1234567890123" },
                )
            }

        assertEquals("Invoice receipt is only available for COMPLETED transactions", error.message)
    }

    @Test
    fun `requires invoice registration number`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                loadInvoiceReceiptView(
                    transactionId = transactionId,
                    loadTransaction = { transaction(status = "COMPLETED") },
                    loadItems = { emptyList() },
                    loadPayments = { emptyList() },
                    loadTaxSummaries = { emptyList() },
                    getInvoiceNumber = { null },
                )
            }

        assertEquals(
            "Invoice registration number is not configured for organization: $organizationId",
            error.message,
        )
    }

    private fun transaction(status: String): TransactionEntity =
        TransactionEntity().apply {
            id = transactionId
            organizationId = this@ReceiptAccessSupportTest.organizationId
            this.status = status
        }
}
