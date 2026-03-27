package com.openpos.pos.event

import com.openpos.pos.entity.OutboxEventEntity
import com.openpos.pos.repository.OutboxRepository
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class OutboxProcessorTest {
    private val emitter: Emitter<String> = mock()
    private val outboxRepository: OutboxRepository = mock()

    private lateinit var outboxProcessor: OutboxProcessor

    @BeforeEach
    fun setUp() {
        outboxProcessor =
            OutboxProcessor().also { processor ->
                val emitterField = OutboxProcessor::class.java.getDeclaredField("emitter")
                emitterField.isAccessible = true
                emitterField.set(processor, emitter)

                val repoField = OutboxProcessor::class.java.getDeclaredField("outboxRepository")
                repoField.isAccessible = true
                repoField.set(processor, outboxRepository)
            }
    }

    private fun createPendingEvent(
        eventType: String,
        payload: String,
    ): OutboxEventEntity =
        OutboxEventEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = UUID.randomUUID()
            this.eventType = eventType
            this.payload = payload
            this.status = "PENDING"
            this.retryCount = 0
        }

    @Test
    fun `PENDINGイベントなし時は何もしない`() {
        // Arrange
        whenever(outboxRepository.findPendingAndMarkInProgress(OutboxProcessor.BATCH_SIZE))
            .thenReturn(emptyList())

        // Act
        outboxProcessor.processOutbox()

        // Assert
        verify(emitter, never()).send(any<Message<String>>())
    }

    @Test
    fun `PENDINGイベントを正常に送信しSENTに更新する`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")
        whenever(outboxRepository.findById(event.id)).thenReturn(event)

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        assertEquals("SENT", event.status)
        assertNotNull(event.sentAt)
        assertEquals(0, event.retryCount)
        verify(outboxRepository).persist(event)
    }

    @Test
    fun `送信失敗時はretryCountをインクリメントしPENDINGに戻す`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")
        whenever(outboxRepository.findById(event.id)).thenReturn(event)
        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        assertEquals("PENDING", event.status)
        assertEquals(1, event.retryCount)
        assertNull(event.sentAt)
        verify(outboxRepository).persist(event)
    }

    @Test
    fun `最大リトライ回数到達でFAILEDに遷移する`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"eventType":"sale.completed"}""")
        event.retryCount = OutboxProcessor.MAX_RETRY_COUNT - 1
        whenever(outboxRepository.findById(event.id)).thenReturn(event)

        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        assertEquals("FAILED", event.status)
        assertEquals(OutboxProcessor.MAX_RETRY_COUNT, event.retryCount)
        verify(outboxRepository).persist(event)
    }

    @Test
    fun `PENDINGイベントがあるとき全件processEventを呼ぶ`() {
        // Arrange
        val event1 = createPendingEvent("sale.completed", """{"id":"1"}""")
        val event2 = createPendingEvent("sale.voided", """{"id":"2"}""")
        whenever(outboxRepository.findPendingAndMarkInProgress(OutboxProcessor.BATCH_SIZE))
            .thenReturn(listOf(event1, event2))
        whenever(outboxRepository.findById(event1.id)).thenReturn(event1)
        whenever(outboxRepository.findById(event2.id)).thenReturn(event2)

        // Act
        outboxProcessor.processOutbox()

        // Assert — 両イベントが送信される
        assertEquals("SENT", event1.status)
        assertEquals("SENT", event2.status)
    }

    @Test
    fun `リトライ9回目まではPENDINGに戻す`() {
        // Arrange
        val event = createPendingEvent("sale.voided", """{"eventType":"sale.voided"}""")
        event.retryCount = OutboxProcessor.MAX_RETRY_COUNT - 2 // 8
        whenever(outboxRepository.findById(event.id)).thenReturn(event)

        doThrow(RuntimeException("RabbitMQ unavailable"))
            .whenever(emitter)
            .send(any<Message<String>>())

        // Act
        outboxProcessor.processEvent(
            event.id.toString(),
            event.eventType,
            event.payload,
            event.retryCount,
        )

        // Assert
        assertEquals("PENDING", event.status)
        assertEquals(OutboxProcessor.MAX_RETRY_COUNT - 1, event.retryCount) // 9
        verify(outboxRepository).persist(event)
    }

    @Test
    fun `claimPendingEventsはfindPendingAndMarkInProgressに委譲する`() {
        // Arrange
        val event = createPendingEvent("sale.completed", """{"id":"1"}""")
        whenever(outboxRepository.findPendingAndMarkInProgress(OutboxProcessor.BATCH_SIZE))
            .thenReturn(listOf(event))

        // Act
        val result = outboxProcessor.claimPendingEvents()

        // Assert
        assertEquals(1, result.size)
        assertEquals(event.id, result[0].id)
        verify(outboxRepository).findPendingAndMarkInProgress(OutboxProcessor.BATCH_SIZE)
    }

    @Test
    fun `存在しないイベントIDの場合は何もしない`() {
        // Arrange
        val nonExistentId = UUID.randomUUID()
        whenever(outboxRepository.findById(nonExistentId)).thenReturn(null)

        // Act
        outboxProcessor.processEvent(
            nonExistentId.toString(),
            "sale.completed",
            """{"eventType":"sale.completed"}""",
            0,
        )

        // Assert
        verify(emitter, never()).send(any<Message<String>>())
        verify(outboxRepository, never()).persist(any<OutboxEventEntity>())
    }
}
