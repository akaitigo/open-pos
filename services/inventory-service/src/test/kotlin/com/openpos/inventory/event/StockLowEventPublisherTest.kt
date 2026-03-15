package com.openpos.inventory.event

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class StockLowEventPublisherTest {
    private val emitter: Emitter<String> = mock()
    private val objectMapper =
        ObjectMapper()
            .registerModule(ParameterNamesModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

    private val publisher =
        StockLowEventPublisher().also { pub ->
            val emitterField = StockLowEventPublisher::class.java.getDeclaredField("emitter")
            emitterField.isAccessible = true
            emitterField.set(pub, emitter)

            val mapperField = StockLowEventPublisher::class.java.getDeclaredField("objectMapper")
            mapperField.isAccessible = true
            mapperField.set(pub, objectMapper)
        }

    @Test
    fun `publish sends StockLowEvent envelope to emitter`() {
        // Arrange
        val orgId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val storeId = UUID.randomUUID()

        // Act
        publisher.publish(
            organizationId = orgId,
            productId = productId,
            storeId = storeId,
            currentQuantity = 5,
            threshold = 10,
        )

        // Assert
        val captor = argumentCaptor<String>()
        verify(emitter).send(captor.capture())

        val json = captor.firstValue
        val envelope = objectMapper.readValue(json, EventEnvelopeDto::class.java)

        assertTrue(envelope.eventId.isNotBlank())
        assertTrue(envelope.eventType == "stock.low")
        assertTrue(envelope.organizationId == orgId.toString())
        assertTrue(envelope.source == "inventory-service")
        assertTrue(envelope.timestamp.isNotBlank())

        val payload = objectMapper.readValue(envelope.payload, StockLowPayload::class.java)
        assertTrue(payload.productId == productId.toString())
        assertTrue(payload.storeId == storeId.toString())
        assertTrue(payload.currentQuantity == 5)
        assertTrue(payload.threshold == 10)
    }

    @Test
    fun `publish generates unique event IDs`() {
        // Arrange
        val orgId = UUID.randomUUID()
        val productId = UUID.randomUUID()
        val storeId = UUID.randomUUID()

        // Act
        publisher.publish(orgId, productId, storeId, 3, 10)
        publisher.publish(orgId, productId, storeId, 2, 10)

        // Assert
        val captor = argumentCaptor<String>()
        verify(emitter, org.mockito.kotlin.times(2)).send(captor.capture())

        val envelope1 = objectMapper.readValue(captor.allValues[0], EventEnvelopeDto::class.java)
        val envelope2 = objectMapper.readValue(captor.allValues[1], EventEnvelopeDto::class.java)

        assertTrue(envelope1.eventId != envelope2.eventId, "Each event should have unique ID")
    }
}
