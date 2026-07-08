package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import io.vertx.core.json.JsonObject
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Optional
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

    /**
     * リトライ回数は AMQP ヘッダ x-openpos-retry-count で管理される（#1259）。
     * retryHeader=null はヘッダなし（初回 DLQ 到着）を表す。
     */
    private fun mockIncomingMetadata(
        retryHeader: Long?,
        contentType: String? = "application/json",
    ): IncomingRabbitMQMetadata {
        val meta = mock<IncomingRabbitMQMetadata>()
        whenever(meta.getHeader(DeadLetterQueueConsumer.RETRY_COUNT_HEADER, Number::class.java))
            .thenReturn(Optional.ofNullable(retryHeader as Number?))
        whenever(meta.contentType).thenReturn(Optional.ofNullable(contentType))
        return meta
    }

    @Suppress("UNCHECKED_CAST")
    private fun mockMessage(
        body: Any,
        incomingMetadata: IncomingRabbitMQMetadata? = mockIncomingMetadata(retryHeader = null),
    ): IncomingRabbitMQMessage<*> {
        val message = mock<IncomingRabbitMQMessage<Any>>()
        whenever(message.payload).thenReturn(body)
        whenever(message.metadata).thenReturn(
            if (incomingMetadata != null) Metadata.of(incomingMetadata) else Metadata.empty(),
        )
        whenever(message.ack()).thenReturn(CompletableFuture.completedFuture(null))
        whenever(message.nack(any<Throwable>())).thenReturn(CompletableFuture.completedFuture(null))
        return message
    }

    private fun buildMessageBody(): String =
        objectMapper.writeValueAsString(
            mapOf(
                "eventId" to "test-event-id",
                "eventType" to "sale.completed",
                "payload" to "{}",
            ),
        )

    private fun sentMessage(emitter: Emitter<String>): Message<String> {
        val captor = argumentCaptor<Message<String>>()
        verify(emitter).send(captor.capture())
        return captor.firstValue
    }

    private fun outgoingMetadataOf(msg: Message<String>): OutgoingRabbitMQMetadata {
        val rabbitMetadata = msg.metadata.get(OutgoingRabbitMQMetadata::class.java)
        assertTrue(rabbitMetadata.isPresent, "OutgoingRabbitMQMetadata should be present")
        return rabbitMetadata.get()
    }

    @Nested
    inner class OnDlqSaleCompleted {
        @Test
        fun `初回（ヘッダなし）はretry=1ヘッダとTTL1秒を付けボディ無加工で再送する`() {
            val body = buildMessageBody()
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = null))

            consumer.onDlqSaleCompleted(message)

            val sent = sentMessage(saleCompletedRetryEmitter)
            assertEquals(body, sent.payload, "リトライボディは受信ボディと同一（無加工）でなければならない")
            val meta = outgoingMetadataOf(sent)
            assertEquals("1000", meta.expiration)
            assertEquals(1L, meta.headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }

        @Test
        fun `ヘッダretry=1は2回目としてTTL5秒で再送する`() {
            val body = buildMessageBody()
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = 1L))

            consumer.onDlqSaleCompleted(message)

            val sent = sentMessage(saleCompletedRetryEmitter)
            val meta = outgoingMetadataOf(sent)
            assertEquals("5000", meta.expiration)
            assertEquals(2L, meta.headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }

        @Test
        fun `ヘッダretry=2は3回目としてTTL25秒で再送する`() {
            val body = buildMessageBody()
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = 2L))

            consumer.onDlqSaleCompleted(message)

            val meta = outgoingMetadataOf(sentMessage(saleCompletedRetryEmitter))
            assertEquals("25000", meta.expiration)
            assertEquals(3L, meta.headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }

        @Test
        fun `ヘッダretry=3（上限到達）は再送せずackする`() {
            val message = mockMessage(buildMessageBody(), mockIncomingMetadata(retryHeader = 3L))

            consumer.onDlqSaleCompleted(message)

            verify(saleCompletedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `parse不能ボディでもヘッダのカウントが進み無限リトライにならない`() {
            // #1259 の核心回帰テスト: ボディが JSON でなくてもヘッダでカウントが管理される
            val garbled = "\"{\\\"escaped\\\": \\\"garbage\\\"}\"" // 二重エンコードで壊れたボディの例
            val message = mockMessage(garbled, mockIncomingMetadata(retryHeader = 2L))

            consumer.onDlqSaleCompleted(message)

            val sent = sentMessage(saleCompletedRetryEmitter)
            assertEquals(garbled, sent.payload, "parse 不能でもボディは無加工のまま")
            assertEquals(3L, outgoingMetadataOf(sent).headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }

        @Test
        fun `parse不能ボディでもヘッダ上限到達なら再送しない`() {
            val message = mockMessage("not valid json at all", mockIncomingMetadata(retryHeader = 3L))

            consumer.onDlqSaleCompleted(message)

            verify(saleCompletedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `JsonObjectペイロードでもリトライできる`() {
            val body = JsonObject(buildMessageBody())
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = null))

            consumer.onDlqSaleCompleted(message)

            val sent = sentMessage(saleCompletedRetryEmitter)
            assertEquals(body.encode(), sent.payload)
            verify(message).ack()
        }

        @Test
        fun `content-typeを元メッセージから引き継ぐ`() {
            val message =
                mockMessage(
                    buildMessageBody(),
                    mockIncomingMetadata(retryHeader = null, contentType = "text/plain"),
                )

            consumer.onDlqSaleCompleted(message)

            assertEquals("text/plain", outgoingMetadataOf(sentMessage(saleCompletedRetryEmitter)).contentType)
        }

        @Test
        fun `content-typeがない場合はapplication_jsonを既定にする`() {
            val message =
                mockMessage(
                    buildMessageBody(),
                    mockIncomingMetadata(retryHeader = null, contentType = null),
                )

            consumer.onDlqSaleCompleted(message)

            assertEquals(
                DeadLetterQueueConsumer.DEFAULT_CONTENT_TYPE,
                outgoingMetadataOf(sentMessage(saleCompletedRetryEmitter)).contentType,
            )
        }

        @Test
        fun `サイズ上限を超えるペイロードは再送せず永久失敗として破棄する`() {
            // フェイルセーフ: RabbitMQ の max_message_size を publish で踏み抜かない
            val huge = "x".repeat(DeadLetterQueueConsumer.MAX_RETRY_PAYLOAD_BYTES + 1)
            val message = mockMessage(huge, mockIncomingMetadata(retryHeader = 0L))

            consumer.onDlqSaleCompleted(message)

            verify(saleCompletedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `サポート外のペイロード型は再送せず永久失敗として破棄する`() {
            val message = mockMessage(12345, mockIncomingMetadata(retryHeader = 0L))

            consumer.onDlqSaleCompleted(message)

            verify(saleCompletedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }
    }

    @Nested
    inner class OnDlqSaleVoided {
        @Test
        fun `初回はsaleVoidedRetryEmitterにretryヘッダ付きで再送する`() {
            val body = buildMessageBody()
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = null))

            consumer.onDlqSaleVoided(message)

            val sent = sentMessage(saleVoidedRetryEmitter)
            assertEquals(body, sent.payload)
            assertEquals(1L, outgoingMetadataOf(sent).headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }

        @Test
        fun `JsonObjectペイロードでもsaleVoidedRetryEmitterに再送する`() {
            val body = JsonObject(buildMessageBody())
            val message = mockMessage(body, mockIncomingMetadata(retryHeader = null))

            consumer.onDlqSaleVoided(message)

            verify(saleVoidedRetryEmitter).send(any<Message<String>>())
            verify(message).ack()
        }

        @Test
        fun `ヘッダ上限到達後は再送しない`() {
            val message = mockMessage(buildMessageBody(), mockIncomingMetadata(retryHeader = 3L))

            consumer.onDlqSaleVoided(message)

            verify(saleVoidedRetryEmitter, never()).send(any<Message<String>>())
            verify(message).ack()
        }
    }

    @Nested
    inner class ErrorHandling {
        @Test
        fun `nacks message when emitter throws exception on sale completed`() {
            val message = mockMessage(buildMessageBody(), mockIncomingMetadata(retryHeader = null))
            whenever(saleCompletedRetryEmitter.send(any<Message<String>>())).thenThrow(RuntimeException("send failed"))

            consumer.onDlqSaleCompleted(message)

            verify(message).nack(any<Throwable>())
        }

        @Test
        fun `nacks message when emitter throws exception on sale voided`() {
            val message = mockMessage(buildMessageBody(), mockIncomingMetadata(retryHeader = null))
            whenever(saleVoidedRetryEmitter.send(any<Message<String>>())).thenThrow(RuntimeException("send failed"))

            consumer.onDlqSaleVoided(message)

            verify(message).nack(any<Throwable>())
        }

        @Test
        fun `RabbitMQメタデータ自体がない場合も初回として扱う`() {
            val body = buildMessageBody()
            val message = mockMessage(body, incomingMetadata = null)

            consumer.onDlqSaleCompleted(message)

            val sent = sentMessage(saleCompletedRetryEmitter)
            assertEquals(1L, outgoingMetadataOf(sent).headers[DeadLetterQueueConsumer.RETRY_COUNT_HEADER])
            verify(message).ack()
        }
    }

    @Nested
    inner class Constants {
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

        @Test
        fun `リトライサイズ上限はRabbitMQ既定の16MiBより十分小さい`() {
            assertTrue(DeadLetterQueueConsumer.MAX_RETRY_PAYLOAD_BYTES < 16 * 1024 * 1024)
        }
    }
}
