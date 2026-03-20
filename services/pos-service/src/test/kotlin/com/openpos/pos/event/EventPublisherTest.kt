package com.openpos.pos.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.pos.repository.OutboxRepository
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class EventPublisherTest {
    private val emitter: Emitter<String> = mock()
    private val objectMapper = ObjectMapper()
    private val outboxRepository: OutboxRepository = mock()

    private lateinit var eventPublisher: EventPublisher

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        eventPublisher =
            EventPublisher().also { publisher ->
                val emitterField = EventPublisher::class.java.getDeclaredField("emitter")
                emitterField.isAccessible = true
                emitterField.set(publisher, emitter)

                val mapperField = EventPublisher::class.java.getDeclaredField("objectMapper")
                mapperField.isAccessible = true
                mapperField.set(publisher, objectMapper)

                val repoField = EventPublisher::class.java.getDeclaredField("outboxRepository")
                repoField.isAccessible = true
                repoField.set(publisher, outboxRepository)
            }
    }

    @Test
    fun `正常送信時はRabbitMQに送信しアウトボックスには保存しない`() {
        // Act
        val eventId = eventPublisher.publish("sale.completed", orgId, mapOf("key" to "value"))

        // Assert
        assertTrue(eventId.toString().isNotBlank())
        verify(emitter).send(any<Message<String>>())
        verify(outboxRepository, never()).persist(any<com.openpos.pos.entity.OutboxEventEntity>())
    }

    @Test
    fun `RabbitMQ送信失敗時はアウトボックスに保存される`() {
        // Arrange
        doThrow(RuntimeException("RabbitMQ connection refused"))
            .whenever(emitter)
            .send(any<Message<String>>())

        // Act
        val eventId = eventPublisher.publish("sale.completed", orgId, mapOf("key" to "value"))

        // Assert
        assertTrue(eventId.toString().isNotBlank())

        val captor = argumentCaptor<com.openpos.pos.entity.OutboxEventEntity>()
        verify(outboxRepository).persist(captor.capture())

        val savedEvent = captor.firstValue
        assertEquals(orgId, savedEvent.organizationId)
        assertEquals("sale.completed", savedEvent.eventType)
        assertEquals("PENDING", savedEvent.status)
        assertEquals(0, savedEvent.retryCount)
        assertTrue(savedEvent.payload.contains("sale.completed"))
    }

    @Test
    fun `複数回の送信失敗でそれぞれアウトボックスに保存される`() {
        // Arrange
        doThrow(RuntimeException("RabbitMQ connection refused"))
            .whenever(emitter)
            .send(any<Message<String>>())

        // Act
        eventPublisher.publish("sale.completed", orgId, mapOf("tx" to "1"))
        eventPublisher.publish("sale.voided", orgId, mapOf("tx" to "2"))

        // Assert
        val captor = argumentCaptor<com.openpos.pos.entity.OutboxEventEntity>()
        verify(outboxRepository, org.mockito.kotlin.times(2)).persist(captor.capture())

        val eventTypes = captor.allValues.map { it.eventType }.toSet()
        assertTrue(eventTypes.contains("sale.completed"))
        assertTrue(eventTypes.contains("sale.voided"))
    }
}
