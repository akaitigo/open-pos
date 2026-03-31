package com.openpos.inventory.event

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import io.smallrye.common.annotation.Blocking
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import io.vertx.core.json.JsonObject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.lang.reflect.Method
import java.util.UUID
import java.util.concurrent.CompletableFuture

class EventConsumerTest {
    private val idempotentHandler: IdempotentEventHandler = mock()
    private val stockEventProcessor: StockEventProcessor = mock()
    private val objectMapper =
        ObjectMapper()
            .registerModule(ParameterNamesModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val consumer =
        EventConsumer().also { consumer ->
            val handlerField = EventConsumer::class.java.getDeclaredField("idempotentHandler")
            handlerField.isAccessible = true
            handlerField.set(consumer, idempotentHandler)

            val processorField = EventConsumer::class.java.getDeclaredField("stockEventProcessor")
            processorField.isAccessible = true
            processorField.set(consumer, stockEventProcessor)

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
        // handleIdempotent のモック: ラムダを実行する
        whenever(idempotentHandler.handleIdempotent(any(), any(), any())).thenAnswer { invocation ->
            val action = invocation.getArgument<() -> Unit>(2)
            action()
            true
        }
    }

    // --- ヘルパー ---

    @Suppress("UNCHECKED_CAST")
    private fun mockMessage(body: Any): IncomingRabbitMQMessage<*> {
        val message = mock<IncomingRabbitMQMessage<Any>>()
        whenever(message.payload).thenReturn(body)
        whenever(message.ack()).thenReturn(CompletableFuture.completedFuture(null))
        whenever(message.nack(any<Throwable>())).thenReturn(CompletableFuture.completedFuture(null))
        return message
    }

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
        fun `onSaleCompletedはblockingで実行される`() {
            val method: Method =
                EventConsumer::class.java.getDeclaredMethod(
                    "onSaleCompleted",
                    IncomingRabbitMQMessage::class.java,
                )

            assertTrue(method.isAnnotationPresent(Blocking::class.java))
        }

        @Test
        fun `正常なJSONでprocessSaleCompletedが呼ばれる`() {
            // Arrange
            val json = buildSaleCompletedJson()
            assertNotNull(json)
            assertTrue(json.contains("sale.completed"))
            val message = mockMessage(json)

            // Act
            consumer.onSaleCompleted(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.completed"), any())
            verify(stockEventProcessor).processSaleCompleted(eq(orgId), any())
            verify(message).ack()
        }

        @Test
        fun `JsonObjectペイロードでもprocessSaleCompletedが呼ばれる`() {
            // Arrange
            val json = buildSaleCompletedJson()
            val jsonObject = JsonObject(json)
            val message = mockMessage(jsonObject)

            // Act
            consumer.onSaleCompleted(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.completed"), any())
            verify(stockEventProcessor).processSaleCompleted(eq(orgId), any())
            verify(message).ack()
        }

        @Test
        fun `不正なJSONではnackが呼ばれる`() {
            // Arrange
            val invalidJson = "{ invalid json }"
            val message = mockMessage(invalidJson)

            // Act
            consumer.onSaleCompleted(message)

            // Assert
            assertNotNull(invalidJson)
            verify(message).nack(any<Throwable>())
        }
    }

    @Nested
    inner class OnSaleVoided {
        @Test
        fun `onSaleVoidedはblockingで実行される`() {
            val method: Method =
                EventConsumer::class.java.getDeclaredMethod(
                    "onSaleVoided",
                    IncomingRabbitMQMessage::class.java,
                )

            assertTrue(method.isAnnotationPresent(Blocking::class.java))
        }

        @Test
        fun `正常なJSONでprocessSaleVoidedが呼ばれる`() {
            // Arrange
            val json = buildSaleVoidedJson()
            assertNotNull(json)
            assertTrue(json.contains("sale.voided"))
            val message = mockMessage(json)

            // Act
            consumer.onSaleVoided(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.voided"), any())
            verify(stockEventProcessor).processSaleVoided(eq(orgId), any())
            verify(message).ack()
        }

        @Test
        fun `JsonObjectペイロードでもprocessSaleVoidedが呼ばれる`() {
            // Arrange
            val json = buildSaleVoidedJson()
            val jsonObject = JsonObject(json)
            val message = mockMessage(jsonObject)

            // Act
            consumer.onSaleVoided(message)

            // Assert
            verify(idempotentHandler).handleIdempotent(eq(eventId), eq("sale.voided"), any())
            verify(stockEventProcessor).processSaleVoided(eq(orgId), any())
            verify(message).ack()
        }

        @Test
        fun `不正なJSONではnackが呼ばれる`() {
            // Arrange
            val invalidJson = "not valid json"
            val message = mockMessage(invalidJson)

            // Act
            consumer.onSaleVoided(message)

            // Assert
            assertNotNull(invalidJson)
            verify(message).nack(any<Throwable>())
        }
    }
}
