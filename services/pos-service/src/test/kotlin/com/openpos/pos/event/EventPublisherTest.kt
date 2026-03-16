package com.openpos.pos.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class EventPublisherTest {
    private val emitter: Emitter<String> = mock()
    private val objectMapper = ObjectMapper()

    private lateinit var eventPublisher: EventPublisher

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        eventPublisher = EventPublisher()
        // emitter フィールドを注入
        val emitterField = EventPublisher::class.java.getDeclaredField("emitter")
        emitterField.isAccessible = true
        emitterField.set(eventPublisher, emitter)
        // objectMapper フィールドを注入
        val mapperField = EventPublisher::class.java.getDeclaredField("objectMapper")
        mapperField.isAccessible = true
        mapperField.set(eventPublisher, objectMapper)
    }

    // === publish ===

    @Nested
    inner class Publish {
        @Test
        fun `イベントを正常に発行しeventIdを返す`() {
            // Arrange
            val payload = mapOf("transactionId" to "tx-123", "amount" to 10000)
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            val eventId = eventPublisher.publish("sale.completed", orgId, payload)

            // Assert
            assertNotNull(eventId)
            verify(emitter).send(messageCaptor.capture())
            assertNotNull(messageCaptor.firstValue.payload)
        }

        @Test
        fun `発行されたメッセージにeventTypeが含まれる`() {
            // Arrange
            val payload = mapOf("id" to "tx-456")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.completed", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertEquals("sale.completed", envelope.get("eventType").asText())
        }

        @Test
        fun `発行されたメッセージにorganizationIdが含まれる`() {
            // Arrange
            val payload = mapOf("id" to "tx-789")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.voided", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertEquals(orgId.toString(), envelope.get("organizationId").asText())
        }

        @Test
        fun `発行されたメッセージにsourceが含まれる`() {
            // Arrange
            val payload = mapOf("reason" to "test")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.voided", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertEquals("pos-service", envelope.get("source").asText())
        }

        @Test
        fun `発行されたメッセージにtimestampが含まれる`() {
            // Arrange
            val payload = mapOf("data" to "value")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.completed", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertNotNull(envelope.get("timestamp").asText())
        }

        @Test
        fun `発行されたメッセージにeventIdが含まれる`() {
            // Arrange
            val payload = mapOf("key" to "val")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            val returnedId = eventPublisher.publish("sale.completed", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertEquals(returnedId.toString(), envelope.get("eventId").asText())
        }

        @Test
        fun `payloadがJSON文字列としてシリアライズされる`() {
            // Arrange
            val payload = mapOf("transactionId" to "tx-001", "total" to 55000)
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.completed", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            val payloadStr = envelope.get("payload").asText()
            val payloadNode = objectMapper.readTree(payloadStr)
            assertEquals("tx-001", payloadNode.get("transactionId").asText())
            assertEquals(55000, payloadNode.get("total").asInt())
        }

        @Test
        fun `異なるeventTypeでもイベントを発行できる`() {
            // Arrange
            val payload = mapOf("reason" to "customer request")
            val messageCaptor = argumentCaptor<Message<String>>()

            // Act
            eventPublisher.publish("sale.voided", orgId, payload)

            // Assert
            verify(emitter).send(messageCaptor.capture())
            val json = messageCaptor.firstValue.payload
            val envelope = objectMapper.readTree(json)
            assertEquals("sale.voided", envelope.get("eventType").asText())
        }
    }
}
