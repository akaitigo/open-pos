package com.openpos.gateway.resource

import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
import io.grpc.Status
import io.grpc.StatusRuntimeException
import openpos.common.v1.PaginationResponse
import openpos.pos.v1.CreateTransactionResponse
import openpos.pos.v1.FinalizeTransactionResponse
import openpos.pos.v1.GetTransactionResponse
import openpos.pos.v1.ListTransactionsResponse
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.Receipt
import openpos.pos.v1.Transaction
import openpos.pos.v1.TransactionStatus
import openpos.pos.v1.TransactionType
import openpos.pos.v1.VoidTransactionResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class TransactionResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val cache: RedisCacheService = mock()
    private val resource =
        TransactionResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "cache", cache)
        }

    private val orgId = UUID.randomUUID().toString()
    private val txId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()
    private val terminalId = UUID.randomUUID().toString()
    private val staffId = UUID.randomUUID().toString()

    private fun buildTransaction(): Transaction =
        Transaction
            .newBuilder()
            .setId(txId)
            .setOrganizationId(orgId)
            .setStoreId(storeId)
            .setTerminalId(terminalId)
            .setStaffId(staffId)
            .setTransactionNumber("T-2026-000001")
            .setType(TransactionType.TRANSACTION_TYPE_SALE)
            .setStatus(TransactionStatus.TRANSACTION_STATUS_DRAFT)
            .setSubtotal(30000)
            .setTaxTotal(3000)
            .setTotal(33000)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class Create {
        @Test
        fun `取引作成で201を返す`() {
            // Arrange
            whenever(stub.createTransaction(any())).thenReturn(
                CreateTransactionResponse.newBuilder().setTransaction(buildTransaction()).build(),
            )
            val body = CreateTransactionBody(storeId = storeId, terminalId = terminalId, staffId = staffId)

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
        }

        @Test
        fun `テナントヘッダー伝播 - gRPC呼び出しにwithTenantが使われる`() {
            // Arrange
            whenever(stub.createTransaction(any())).thenReturn(
                CreateTransactionResponse.newBuilder().setTransaction(buildTransaction()).build(),
            )
            val body = CreateTransactionBody(storeId = storeId, terminalId = terminalId, staffId = staffId)

            // Act
            resource.create(body)

            // Assert
            verify(grpc).withTenant(stub)
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `取引取得でMapを返す`() {
            // Arrange
            whenever(stub.getTransaction(any())).thenReturn(
                GetTransactionResponse.newBuilder().setTransaction(buildTransaction()).build(),
            )

            // Act
            val result = resource.get(txId)

            // Assert
            assertEquals("T-2026-000001", result["transactionNumber"])
            assertEquals("SALE", result["type"])
            assertEquals("DRAFT", result["status"])
        }
    }

    @Nested
    inner class List {
        @Test
        fun `取引一覧でページネーション付きMapを返す`() {
            // Arrange
            whenever(stub.listTransactions(any())).thenReturn(
                ListTransactionsResponse
                    .newBuilder()
                    .addTransactions(buildTransaction())
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(1)
                            .setTotalPages(1)
                            .build(),
                    ).build(),
            )

            // Act
            val result =
                resource.list(
                    page = 1,
                    pageSize = 20,
                    storeId = null,
                    terminalId = null,
                    status = null,
                    startDate = null,
                    endDate = null,
                )

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as kotlin.collections.List<*>
            assertEquals(1, data.size)
        }

        @Test
        fun `日付範囲フィルター付きで取引一覧`() {
            // Arrange
            whenever(stub.listTransactions(any())).thenReturn(
                ListTransactionsResponse
                    .newBuilder()
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(0)
                            .setTotalPages(0)
                            .build(),
                    ).build(),
            )

            // Act
            val result =
                resource.list(
                    page = 1,
                    pageSize = 20,
                    storeId = storeId,
                    terminalId = null,
                    status = "COMPLETED",
                    startDate = "2026-01-01T00:00:00Z",
                    endDate = "2026-01-31T23:59:59Z",
                )

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as kotlin.collections.List<*>
            assertEquals(0, data.size)
        }
    }

    @Nested
    inner class Finalize {
        @Test
        fun `取引確定でtransactionとreceiptを返す`() {
            // Arrange
            val tx = buildTransaction().toBuilder().setStatus(TransactionStatus.TRANSACTION_STATUS_COMPLETED).build()
            val receipt =
                Receipt
                    .newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setTransactionId(txId)
                    .setReceiptData("receipt data")
                    .setCreatedAt("2026-01-01T00:00:00Z")
                    .build()
            whenever(stub.finalizeTransaction(any())).thenReturn(
                FinalizeTransactionResponse
                    .newBuilder()
                    .setTransaction(tx)
                    .setReceipt(receipt)
                    .build(),
            )
            val body = FinalizeBody(payments = listOf(PaymentInputBody(method = "CASH", amount = 33000, received = 50000)))

            // Act
            val result = resource.finalize(txId, null, body)

            // Assert
            assertEquals(true, result.containsKey("transaction"))
            assertEquals(true, result.containsKey("receipt"))
            verify(cache).invalidatePattern("openpos:gateway:transaction:*")
        }

        @Test
        fun `冪等性キー付きで取引確定`() {
            // Arrange
            val tx = buildTransaction().toBuilder().setStatus(TransactionStatus.TRANSACTION_STATUS_COMPLETED).build()
            val receipt =
                Receipt
                    .newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setTransactionId(txId)
                    .setReceiptData("receipt data")
                    .setCreatedAt("2026-01-01T00:00:00Z")
                    .build()
            whenever(stub.finalizeTransaction(any())).thenReturn(
                FinalizeTransactionResponse
                    .newBuilder()
                    .setTransaction(tx)
                    .setReceipt(receipt)
                    .build(),
            )
            val idempotencyKey = UUID.randomUUID().toString()
            whenever(grpc.withIdempotencyKey(stub, idempotencyKey)).thenReturn(stub)
            val body = FinalizeBody(payments = listOf(PaymentInputBody(method = "CASH", amount = 33000, received = 50000)))

            // Act
            val result = resource.finalize(txId, idempotencyKey, body)

            // Assert
            assertEquals(true, result.containsKey("transaction"))
            verify(grpc).withIdempotencyKey(stub, idempotencyKey)
        }
    }

    @Nested
    inner class Void {
        @Test
        fun `取引無効化でMapを返す`() {
            // Arrange
            val tx = buildTransaction().toBuilder().setStatus(TransactionStatus.TRANSACTION_STATUS_VOIDED).build()
            whenever(stub.voidTransaction(any())).thenReturn(
                VoidTransactionResponse.newBuilder().setTransaction(tx).build(),
            )

            // Act
            val result = resource.void(txId, VoidBody(reason = "誤操作"))

            // Assert
            assertEquals("VOIDED", result["status"])
            verify(cache).invalidatePattern("openpos:gateway:transaction:*")
        }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `gRPCエラーはStatusRuntimeExceptionとして伝播`() {
            // Arrange
            whenever(stub.getTransaction(any())).thenThrow(
                StatusRuntimeException(Status.NOT_FOUND.withDescription("Transaction not found")),
            )

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                resource.get("nonexistent")
            }
        }
    }
}
