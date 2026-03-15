package com.openpos.analytics.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify

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

    private fun buildMessage(retryCount: Int? = null): String {
        val map =
            mutableMapOf<String, Any>(
                "eventId" to "test-event-id",
                "eventType" to "sale.completed",
                "payload" to "{}",
            )
        retryCount?.let { map["retryCount"] = it }
        return objectMapper.writeValueAsString(map)
    }

    @Nested
    inner class OnDlqSaleCompleted {
        @Test
        fun `初回リトライ時はsaleCompletedRetryEmitterに再送する`() {
            // Arrange
            val message = buildMessage(retryCount = 0)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val captor = argumentCaptor<String>()
            verify(saleCompletedRetryEmitter).send(captor.capture())

            val sentMessage = objectMapper.readValue(captor.firstValue, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 1)
        }

        @Test
        fun `retryCountなしの場合は0として扱いリトライする`() {
            // Arrange
            val message = buildMessage(retryCount = null)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val captor = argumentCaptor<String>()
            verify(saleCompletedRetryEmitter).send(captor.capture())

            val sentMessage = objectMapper.readValue(captor.firstValue, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 1)
        }

        @Test
        fun `最大リトライ回数到達後は再送しない`() {
            // Arrange
            val message = buildMessage(retryCount = 3)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            verify(saleCompletedRetryEmitter, never()).send(org.mockito.kotlin.any())
        }

        @Test
        fun `2回目のリトライでretryCountが正しくインクリメントされる`() {
            // Arrange
            val message = buildMessage(retryCount = 1)

            // Act
            consumer.onDlqSaleCompleted(message)

            // Assert
            val captor = argumentCaptor<String>()
            verify(saleCompletedRetryEmitter).send(captor.capture())

            val sentMessage = objectMapper.readValue(captor.firstValue, Map::class.java)
            assertTrue(sentMessage["retryCount"] == 2)
        }
    }

    @Nested
    inner class OnDlqSaleVoided {
        @Test
        fun `初回リトライ時はsaleVoidedRetryEmitterに再送する`() {
            // Arrange
            val message = buildMessage(retryCount = 0)

            // Act
            consumer.onDlqSaleVoided(message)

            // Assert
            verify(saleVoidedRetryEmitter).send(org.mockito.kotlin.any())
        }

        @Test
        fun `最大リトライ回数到達後は再送しない`() {
            // Arrange
            val message = buildMessage(retryCount = 3)

            // Act
            consumer.onDlqSaleVoided(message)

            // Assert
            verify(saleVoidedRetryEmitter, never()).send(org.mockito.kotlin.any())
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
