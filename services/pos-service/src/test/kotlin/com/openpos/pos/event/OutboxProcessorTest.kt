package com.openpos.pos.event

import com.openpos.pos.entity.OutboxEventEntity
import com.openpos.pos.repository.OutboxRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class OutboxProcessorTest {
    @Inject
    lateinit var outboxProcessor: OutboxProcessor

    @Inject
    lateinit var outboxRepository: OutboxRepository

    @InjectMock
    lateinit var emitter: Emitter<String>

    @BeforeEach
    @Transactional
    fun setUp() {
        outboxRepository.deleteAll()
    }

    @Test
    fun `PENDINGイベントなし時は何もしない`() {
        // Act
        outboxProcessor.processOutbox()

        // Assert
        val events = outboxRepository.findPendingEvents(10)
        assertEquals(0, events.size)
    }

    @Test
    @Transactional
    fun `PENDINGイベントを正常に送信しSENTに更新する`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        val updated = outboxRepository.findById(event.id)
        assertNotNull(updated)
        assertEquals("SENT", requireNotNull(updated).status)
        assertNotNull(updated.sentAt)
        assertEquals(0, updated.retryCount)
    }

    @Test
    @Transactional
    fun `送信失敗時はretryCountをインクリメントしPENDINGのまま`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")
        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        val updated = outboxRepository.findById(event.id)
        assertNotNull(updated)
        assertEquals("PENDING", requireNotNull(updated).status)
        assertEquals(1, updated.retryCount)
        assertNull(updated.sentAt)
    }

    @Test
    @Transactional
    fun `最大リトライ回数到達でFAILEDに遷移する`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")
        event.retryCount = OutboxProcessor.MAX_RETRY_COUNT - 1
        outboxRepository.persist(event)

        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        val updated = outboxRepository.findById(event.id)
        assertNotNull(updated)
        assertEquals("FAILED", requireNotNull(updated).status)
        assertEquals(OutboxProcessor.MAX_RETRY_COUNT, updated.retryCount)
    }

    @Test
    @Transactional
    fun `リトライ9回目まではPENDINGを維持する`() {
        // Arrange
        val event = createPendingEvent("sale.voided", """{"eventType":"sale.voided"}""")
        event.retryCount = OutboxProcessor.MAX_RETRY_COUNT - 2 // 8
        outboxRepository.persist(event)

        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<org.eclipse.microprofile.reactive.messaging.Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        val updated = outboxRepository.findById(event.id)
        assertNotNull(updated)
        assertEquals("PENDING", requireNotNull(updated).status)
        assertEquals(OutboxProcessor.MAX_RETRY_COUNT - 1, updated.retryCount) // 9
    }

    // === Helper ===

    private fun createPendingEvent(
        eventType: String,
        payload: String,
    ): OutboxEventEntity {
        val event =
            OutboxEventEntity().apply {
                this.eventType = eventType
                this.payload = payload
                this.status = "PENDING"
                this.retryCount = 0
            }
        outboxRepository.persist(event)
        return event
    }
}
