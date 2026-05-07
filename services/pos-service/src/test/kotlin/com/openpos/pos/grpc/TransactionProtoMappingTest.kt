package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import io.grpc.Status
import openpos.pos.v1.PaymentMethod
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class TransactionProtoMappingTest {
    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val transactionId = UUID.fromString("11111111-1111-1111-1111-111111111111")

    @Test
    fun `transaction proto includes nested entities and enum mappings`() {
        val transaction =
            transaction().toProto(
                items = items(),
                payments = payments(),
                discounts = discounts(),
                taxSummaries = taxSummaries(),
            )

        assertEquals(transactionId.toString(), transaction.id)
        assertEquals(TransactionType.TRANSACTION_TYPE_SALE, transaction.type)
        assertEquals(TransactionStatus.TRANSACTION_STATUS_COMPLETED, transaction.status)
        assertEquals(1, transaction.itemsCount)
        assertEquals(1, transaction.paymentsCount)
        assertEquals(1, transaction.discountsCount)
        assertEquals(1, transaction.taxSummariesCount)
        assertEquals(PaymentMethod.PAYMENT_METHOD_CASH, transaction.paymentsList.single().method)
        assertEquals("テスト商品A", transaction.itemsList.single().productName)
        assertEquals("値引き", transaction.discountsList.single().name)
    }

    @Test
    fun `unknown db values map to unspecified enums`() {
        assertEquals(TransactionType.TRANSACTION_TYPE_UNSPECIFIED, "OTHER".toProtoTransactionType())
        assertEquals(TransactionStatus.TRANSACTION_STATUS_UNSPECIFIED, "PENDING".toProtoTransactionStatus())
        assertEquals(PaymentMethod.PAYMENT_METHOD_UNSPECIFIED, "POINTS".toProtoPaymentMethod())
    }

    @Test
    fun `unspecified payment method is rejected when converting to db value`() {
        val error =
            assertThrows(io.grpc.StatusRuntimeException::class.java) {
                PaymentMethod.PAYMENT_METHOD_UNSPECIFIED.toDbValue()
            }

        assertEquals(Status.Code.INVALID_ARGUMENT, Status.fromThrowable(error).code)
    }

    private fun transaction(): TransactionEntity =
        TransactionEntity().apply {
            id = transactionId
            organizationId = this@TransactionProtoMappingTest.organizationId
            storeId = UUID.fromString("22222222-2222-2222-2222-222222222222")
            terminalId = UUID.fromString("33333333-3333-3333-3333-333333333333")
            staffId = UUID.fromString("44444444-4444-4444-4444-444444444444")
            transactionNumber = "T-2026-000002"
            type = "SALE"
            status = "COMPLETED"
            clientId = "client-123"
            subtotal = 10000
            taxTotal = 1000
            discountTotal = 500
            total = 10500
            changeAmount = 0
            createdAt = Instant.parse("2026-05-07T01:00:00Z")
            updatedAt = Instant.parse("2026-05-07T01:05:00Z")
            completedAt = Instant.parse("2026-05-07T01:05:00Z")
        }

    private fun items(): List<TransactionItemEntity> =
        listOf(
            TransactionItemEntity().apply {
                id = UUID.fromString("55555555-5555-5555-5555-555555555555")
                organizationId = this@TransactionProtoMappingTest.organizationId
                transactionId = this@TransactionProtoMappingTest.transactionId
                productId = UUID.fromString("66666666-6666-6666-6666-666666666666")
                productName = "テスト商品A"
                unitPrice = 10000
                quantity = 1
                taxRateName = "標準税率10%"
                taxRate = "0.10"
                subtotal = 10000
                taxAmount = 1000
                total = 11000
            },
        )

    private fun payments(): List<PaymentEntity> =
        listOf(
            PaymentEntity().apply {
                id = UUID.fromString("77777777-7777-7777-7777-777777777777")
                organizationId = this@TransactionProtoMappingTest.organizationId
                transactionId = this@TransactionProtoMappingTest.transactionId
                method = "CASH"
                amount = 10500
                received = 10500
                change = 0
            },
        )

    private fun discounts(): List<TransactionDiscountEntity> =
        listOf(
            TransactionDiscountEntity().apply {
                id = UUID.fromString("88888888-8888-8888-8888-888888888888")
                organizationId = this@TransactionProtoMappingTest.organizationId
                transactionId = this@TransactionProtoMappingTest.transactionId
                name = "値引き"
                discountType = "FIXED_AMOUNT"
                value = "500"
                amount = 500
            },
        )

    private fun taxSummaries(): List<TaxSummaryEntity> =
        listOf(
            TaxSummaryEntity().apply {
                id = UUID.fromString("99999999-9999-9999-9999-999999999999")
                organizationId = this@TransactionProtoMappingTest.organizationId
                transactionId = this@TransactionProtoMappingTest.transactionId
                taxRateName = "標準税率10%"
                taxRate = "0.10"
                taxableAmount = 10000
                taxAmount = 1000
            },
        )
}
