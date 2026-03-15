package com.openpos.analytics.event

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class EventConsumerTest {
    private val idempotentHandler: IdempotentEventHandler = mock()
    private val salesEventProcessor: SalesEventProcessor = mock()
    private val objectMapper =
        ObjectMapper()
            .registerModule(ParameterNamesModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val consumer =
        EventConsumer().also { consumer ->
            val handlerField = EventConsumer::class.java.getDeclaredField("idempotentHandler")
            handlerField.isAccessible = true
            handlerField.set(consumer, idempotentHandler)

            val processorField = EventConsumer::class.java.getDeclaredField("salesEventProcessor")
            processorField.isAccessible = true
            processorField.set(consumer, salesEventProcessor)

            val mapperField = EventConsumer::class.java.getDeclaredField("objectMapper")
            mapperField.isAccessible = true
            mapperField.set(consumer, objectMapper)
        }

    private val orgId = UUID.randomUUID()
    private val eventId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId1 = UUID.randomUUID()
    private val productId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(idempotentHandler.handleIdempotent(any(), any(), any())).thenAnswer { invocation ->
            val action = invocation.getArgument<() -> Unit>(2)
            action()
            true
        }
    }

    // --- ヘルパー ---

    private fun buildSaleCompletedJson(
        eventId: UUID = this.eventId,
        orgId: UUID = this.orgId,
        transactionId: String = "txn-001",
        storeId: UUID = this.storeId,
        items: List<SaleItemPayload> =
            listOf(
                SaleItemPayload(productId1.toString(), 2, 10000, 20000),
                SaleItemPayload(productId2.toString(), 1, 5000, 5000),
            ),
    ): String {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "transactionId" to transactionId,
                    "storeId" to storeId.toString(),
                    "terminalId" to "terminal-1",
                    "items" to
                        items.map {
                            mapOf(
                                "productId" to it.productId,
                                "quantity" to it.quantity,
                                "unitPrice" to it.unitPrice,
                                "subtotal" to it.subtotal,
                            )
                        },
                    "totalAmount" to 25000,
                    "transactedAt" to "2026-03-06T10:00:00Z",
                ),
            )
        val envelope =
            EventEnvelopeDto(
                eventId = eventId.toString(),
                eventType = "sale.completed",
                timestamp = "2026-03-06T10:00:00Z",
                organizationId = orgId.toString(),
                payload = payload,
                source = "pos-service",
            )
        return objectMapper.writeValueAsString(envelope)
    }

    private fun buildSaleVoidedJson(
        eventId: UUID = this.eventId,
        orgId: UUID = this.orgId,
        originalTransactionId: String = "txn-001",
        voidTransactionId: String = "void-001",
        storeId: UUID = this.storeId,
        items: List<SaleItemPayload> =
            listOf(
                SaleItemPayload(productId1.toString(), 2, 10000, 20000),
                SaleItemPayload(productId2.toString(), 3, 5000, 15000),
            ),
    ): String {
        val payload =
            objectMapper.writeValueAsString(
                mapOf(
                    "originalTransactionId" to originalTransactionId,
                    "voidTransactionId" to voidTransactionId,
                    "storeId" to storeId.toString(),
                    "items" to
                        items.map {
                            mapOf(
                                "productId" to it.productId,
                                "quantity" to it.quantity,
                                "unitPrice" to it.unitPrice,
                                "subtotal" to it.subtotal,
                            )
                        },
                ),
            )
        val envelope =
            EventEnvelopeDto(
                eventId = eventId.toString(),
                eventType = "sale.voided",
                timestamp = "2026-03-06T10:00:00Z",
                organizationId = orgId.toString(),
                payload = payload,
                source = "pos-service",
            )
        return objectMapper.writeValueAsString(envelope)
    }

    // --- テスト ---

    @Nested
    inner class OnSaleCompleted {
        @Test
        fun `正常なJSONでprocessSaleCompletedが呼ばれる`() {
            // Arrange
            val message = buildSaleCompletedJson()

            // Act
            consumer.onSaleCompleted(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.completed"), any())
            verify(salesEventProcessor).processSaleCompleted(eq(orgId), any())
        }

        @Test
        fun `不正なJSONでは例外がスローされる`() {
            // Arrange
            val invalidJson = "{ invalid json }"

            // Act & Assert
            assertThrows(Exception::class.java) {
                consumer.onSaleCompleted(invalidJson)
            }
        }
    }

    @Nested
    inner class OnSaleVoided {
        @Test
        fun `正常なJSONでprocessSaleVoidedが呼ばれる`() {
            // Arrange
            val message = buildSaleVoidedJson()

            // Act
            consumer.onSaleVoided(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.voided"), any())
            verify(salesEventProcessor).processSaleVoided(eq(orgId), any())
        }

        @Test
        fun `不正なJSONでは例外がスローされる`() {
            // Arrange
            val invalidJson = "not valid json"

            // Act & Assert
            assertThrows(Exception::class.java) {
                consumer.onSaleVoided(invalidJson)
            }
        }
    }
}
