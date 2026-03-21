package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.concurrent.CompletableFuture

class DeadLetterQueueConsumerTest {
    private val objectMapper = ObjectMapper()
    private val saleCompletedRetryEmitter: Emitter<String> = mock()
    private val saleVoidedRetryEmitter: Emitter<String> = mock()

    private val consumer =
        DeadLetterQueueConsumer().also { c ->
            val mapperField = DeadLetterQueueConsumer::class.java.getDeclaredField("objectMapper")
            mapperField.isAccessible = true
            mapperField.set(c, objectMapper)

            val completedField = DeadLetterQueueConsumer::class.java.getDeclaredField("saleCompletedRetryEmitter")
            completedField.isAccessible = true
            completedField.set(c, saleCompletedRetryEmitter)

            val voidedField = DeadLetterQueueConsumer::class.java.getDeclaredField("saleVoidedRetryEmitter")
            voidedField.isAccessible = true
            voidedField.set(c, saleVoidedRetryEmitter)
        }

    @Suppress("UNCHECKED_CAST")
    private fun mockMessage(body: String): IncomingRabbitMQMessage<String> {
        val message = mock<IncomingRabbitMQMessage<String>>()
        whenever(message.payload).thenReturn(body)
        whenever(message.ack()).thenReturn(CompletableFuture.completedFuture(null))
        whenever(message.nack(any<Throwable>())).thenReturn(CompletableFuture.completedFuture(null))
        return message
    }

    private fun buildMessageBody(retryCount: Int? = null): String {
        val map =
            mutableMapOf<String, Any>(
                "eventId" to "test-event-id",
                "eventType" to "sale.completed",
                "payload" to "{}",
            )
        retryCount?.let { map["retryCount"] = it }
        return objectMapper.writeValueAsString(map)
    }

    @Suppress("UNCHECKED_CAST")
    private fun captureMessagePayload(emitter: Emitter<String>): String {
        val captor = ArgumentCaptor.forClass(Message::class.java) as ArgumentCaptor<Message<String>>
        verify(emitter).send(captor.capture())
        return captor.value.payload
    }

    @Nested
    inner class OnDlqSaleCompleted {
        @Test
        fun `初回リトライ時はsaleCompletedRetryEmitterに再送する`() {
            // Arrange
            val body = buildMessageBody(retryCount = 0)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val payload = captureMessagePayload(saleCompletedRetryEmitter)
            val sentMessage = objectMapper.readValue(payload, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 1)
            verify(message).ack()
        }

        @Test
        fun `retryCountなしの場合は0として扱いリトライする`() {
            // Arrange
            val body = buildMessageBody(retryCount = null)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val payload = captureMessagePayload(saleCompletedRetryEmitter)
            val sentMessage = objectMapper.readValue(payload, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 1)
            verify(message).ack()
        }

        @Test
        fun `最大リトライ回数到達後は再送しない`() {
            // Arrange
            val body = buildMessageBody(retryCount = 3)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            verify(saleCompletedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `2回目のリトライでretryCountが正しくインクリメントされる`() {
            // Arrange
            val body = buildMessageBody(retryCount = 1)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val payload = captureMessagePayload(saleCompletedRetryEmitter)
            val sentMessage = objectMapper.readValue(payload, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 2)
            verify(message).ack()
        }
    }

    @Nested
    inner class OnDlqSaleVoided {
        @Test
        fun `初回リトライ時はsaleVoidedRetryEmitterに再送する`() {
            // Arrange
            val body = buildMessageBody(retryCount = 0)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleVoided(message)

            // Assert
            verify(saleVoidedRetryEmitter).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `最大リトライ回数到達後は再送しない`() {
            // Arrange
            val body = buildMessageBody(retryCount = 3)
            val message = mockMessage(body)

            // Act
            consumer.onDlqSaleVoided(message)

            // Assert
            verify(saleVoidedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }
    }

    @Nested
    inner class MaxRetryCount {
        @Test
        fun `MAX_RETRY_COUNTは3である`() {
            assertTrue(DeadLetterQueueConsumer.MAX_RETRY_COUNT == 3)
        }

        @Test
        fun `バックオフ遅延は1秒、5秒、25秒の指数バックオフである`() {
            val delays = DeadLetterQueueConsumer.BACKOFF_DELAYS_MS
            assertTrue(delays[0] == 1_000L)
            assertTrue(delays[1] == 5_000L)
            assertTrue(delays[2] == 25_000L)
        }
    }
}
