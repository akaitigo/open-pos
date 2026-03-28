package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import jakarta.ws.rs.BadRequestException
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.SyncOfflineTransactionsResponse
import openpos.pos.v1.SyncResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class SyncResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        SyncResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", TenantContext())
            ProductResourceTest.setField(r, "maxUnitPrice", 10_000_000L)
        }

    private val clientId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()
    private val terminalId = UUID.randomUUID().toString()
    private val staffId = UUID.randomUUID().toString()

    private fun buildItem(
        unitPrice: Long = 10000,
        productId: String = UUID.randomUUID().toString(),
    ) = OfflineItemBody(
        productId = productId,
        productName = "テスト商品",
        unitPrice = unitPrice,
        quantity = 1,
        taxRateName = "標準税率",
        taxRate = "0.10",
        isReducedTax = false,
    )

    private fun buildPayment(amount: Long = 10000) = PaymentInputBody(method = "CASH", amount = amount, received = amount)

    private fun buildTransaction(items: List<OfflineItemBody> = listOf(buildItem())) =
        OfflineTransactionBody(
            clientId = clientId,
            storeId = storeId,
            terminalId = terminalId,
            staffId = staffId,
            items = items,
            payments = listOf(buildPayment()),
        )

    private fun buildBody(transactions: List<OfflineTransactionBody> = listOf(buildTransaction())) =
        SyncTransactionsBody(transactions = transactions)

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class ValidPrice {
        @Test
        fun `正常な価格でsync成功`() {
            // Arrange
            whenever(stub.syncOfflineTransactions(any())).thenReturn(
                SyncOfflineTransactionsResponse
                    .newBuilder()
                    .addResults(
                        SyncResult
                            .newBuilder()
                            .setClientId(clientId)
                            .setSuccess(true)
                            .setTransactionId(UUID.randomUUID().toString())
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.syncTransactions(buildBody())

            // Assert
            @Suppress("UNCHECKED_CAST")
            val results = result["results"] as List<Map<String, Any?>>
            assertEquals(1, results.size)
            assertEquals(true, results[0]["success"])
        }

        @Test
        fun `1銭（最小有効価格）でsync成功`() {
            // Arrange
            whenever(stub.syncOfflineTransactions(any())).thenReturn(
                SyncOfflineTransactionsResponse
                    .newBuilder()
                    .addResults(
                        SyncResult
                            .newBuilder()
                            .setClientId(clientId)
                            .setSuccess(true)
                            .setTransactionId(UUID.randomUUID().toString())
                            .build(),
                    ).build(),
            )

            // Act
            val result =
                resource.syncTransactions(
                    buildBody(listOf(buildTransaction(items = listOf(buildItem(unitPrice = 1))))),
                )

            // Assert
            @Suppress("UNCHECKED_CAST")
            val results = result["results"] as List<Map<String, Any?>>
            assertEquals(1, results.size)
        }

        @Test
        fun `上限ちょうどの価格でsync成功`() {
            // Arrange
            whenever(stub.syncOfflineTransactions(any())).thenReturn(
                SyncOfflineTransactionsResponse
                    .newBuilder()
                    .addResults(
                        SyncResult
                            .newBuilder()
                            .setClientId(clientId)
                            .setSuccess(true)
                            .setTransactionId(UUID.randomUUID().toString())
                            .build(),
                    ).build(),
            )

            // Act
            val result =
                resource.syncTransactions(
                    buildBody(listOf(buildTransaction(items = listOf(buildItem(unitPrice = 10_000_000))))),
                )

            // Assert
            @Suppress("UNCHECKED_CAST")
            val results = result["results"] as List<Map<String, Any?>>
            assertEquals(1, results.size)
        }
    }

    @Nested
    inner class ZeroPrice {
        @Test
        fun `unitPrice=0でBadRequestException`() {
            // Arrange
            val body = buildBody(listOf(buildTransaction(items = listOf(buildItem(unitPrice = 0)))))

            // Act & Assert
            val ex =
                assertThrows<BadRequestException> {
                    resource.syncTransactions(body)
                }
            assertEquals(true, ex.message?.contains("must be greater than 0"))
        }
    }

    @Nested
    inner class NegativePrice {
        @Test
        fun `unitPrice=-1でBadRequestException`() {
            // Arrange
            val body = buildBody(listOf(buildTransaction(items = listOf(buildItem(unitPrice = -1)))))

            // Act & Assert
            val ex =
                assertThrows<BadRequestException> {
                    resource.syncTransactions(body)
                }
            assertEquals(true, ex.message?.contains("must be greater than 0"))
        }

        @Test
        fun `大きな負の値でBadRequestException`() {
            // Arrange
            val body =
                buildBody(
                    listOf(buildTransaction(items = listOf(buildItem(unitPrice = -999_999_999)))),
                )

            // Act & Assert
            assertThrows<BadRequestException> {
                resource.syncTransactions(body)
            }
        }
    }

    @Nested
    inner class ExceedsMaxPrice {
        @Test
        fun `上限超過でBadRequestException`() {
            // Arrange
            val body =
                buildBody(
                    listOf(buildTransaction(items = listOf(buildItem(unitPrice = 10_000_001)))),
                )

            // Act & Assert
            val ex =
                assertThrows<BadRequestException> {
                    resource.syncTransactions(body)
                }
            assertEquals(true, ex.message?.contains("exceeds maximum allowed price"))
        }

        @Test
        fun `大幅な上限超過でBadRequestException`() {
            // Arrange
            val body =
                buildBody(
                    listOf(buildTransaction(items = listOf(buildItem(unitPrice = Long.MAX_VALUE)))),
                )

            // Act & Assert
            assertThrows<BadRequestException> {
                resource.syncTransactions(body)
            }
        }
    }

    @Nested
    inner class MultipleItems {
        @Test
        fun `複数アイテムのうち1つが不正な場合BadRequestException`() {
            // Arrange
            val items =
                listOf(
                    buildItem(unitPrice = 10000),
                    buildItem(unitPrice = -1),
                )
            val body = buildBody(listOf(buildTransaction(items = items)))

            // Act & Assert
            assertThrows<BadRequestException> {
                resource.syncTransactions(body)
            }
        }

        @Test
        fun `複数アイテムのうち1つが上限超過の場合BadRequestException`() {
            // Arrange
            val items =
                listOf(
                    buildItem(unitPrice = 10000),
                    buildItem(unitPrice = 10_000_001),
                )
            val body = buildBody(listOf(buildTransaction(items = items)))

            // Act & Assert
            assertThrows<BadRequestException> {
                resource.syncTransactions(body)
            }
        }
    }
}
