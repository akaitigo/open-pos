package com.openpos.pos.event

import com.openpos.pos.entity.OutboxEventEntity
import com.openpos.pos.repository.OutboxRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class EventPublisherTest {
    @Inject
    lateinit var eventPublisher: EventPublisher

    @Inject
    lateinit var outboxRepository: OutboxRepository

    @InjectMock
    lateinit var emitter: Emitter<String>

    private val orgId = UUID.randomUUID()

    @BeforeEach
    @Transactional
    fun setUp() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `正常送信時はRabbitMQに送信しアウトボックスには保存しない`() {
        // Act
        val eventId = eventPublisher.publish("sale.completed", orgId, mapOf("key" to "value"))

        // Assert
        assertTrue(eventId.toString().isNotBlank())
        verify(emitter).send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        val pendingEvents = outboxRepository.findPendingEvents(10)
        assertEquals(0, pendingEvents.size)
    }

    @Test
    fun `RabbitMQ送信失敗時はアウトボックスに保存される`() {
        // Arrange
        doThrow(RuntimeException("RabbitMQ connection refused"))
            .whenever(emitter)
            .send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        // Act
        val eventId = eventPublisher.publish("sale.completed", orgId, mapOf("key" to "value"))

        // Assert
        assertTrue(eventId.toString().isNotBlank())

        val pendingEvents = outboxRepository.findPendingEvents(10)
        assertEquals(1, pendingEvents.size)
        val savedEvent = pendingEvents[0]
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
            .send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        // Act
        eventPublisher.publish("sale.completed", orgId, mapOf("tx" to "1"))
        eventPublisher.publish("sale.voided", orgId, mapOf("tx" to "2"))

        // Assert
        val pendingEvents = outboxRepository.findPendingEvents(10)
        assertEquals(2, pendingEvents.size)

        val eventTypes = pendingEvents.map { it.eventType }.toSet()
        assertTrue(eventTypes.contains("sale.completed"))
        assertTrue(eventTypes.contains("sale.voided"))
    }
}
