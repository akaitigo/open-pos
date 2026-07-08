package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import io.vertx.core.json.JsonObject
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.jboss.logging.Logger
import java.nio.charset.StandardCharsets
import java.util.concurrent.CompletionStage

/**
 * inventory-service の Dead Letter Queue コンシューマー。
 * DLQ に到着したメッセージを監視し、リトライ可能な場合は元のキューに再送する。
 * 最大 3 回のリトライ（指数バックオフ: 1s, 5s, 25s）を試み、
 * それを超えた場合は永久失敗としてログに記録する。
 * 明示的に ack/nack を行う。
 *
 * バックオフは Thread.sleep ではなく RabbitMQ メッセージの expiration（TTL）で実現する。
 * TTL 付きメッセージは RabbitMQ 側で遅延後に配信されるため、スレッドをブロックしない。
 *
 * リトライ回数は AMQP ヘッダ [RETRY_COUNT_HEADER] で管理する。
 * ボディ埋め込み方式はボディが JSON として parse 不能になった時点でカウントを見失い、
 * 「再エンコードによる肥大化 × 無限リトライ」で RabbitMQ の max message size（既定 16MiB）に
 * 到達してチャネルが恒久 DOWN する事故を起こした（#1259）。ボディは一切加工せず再送する。
 */
@ApplicationScoped
class DeadLetterQueueConsumer {
    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    @Channel("sale-completed-retry")
    lateinit var saleCompletedRetryEmitter: Emitter<String>

    @Inject
    @Channel("sale-voided-retry")
    lateinit var saleVoidedRetryEmitter: Emitter<String>

    private val log = Logger.getLogger(DeadLetterQueueConsumer::class.java)

    companion object {
        const val MAX_RETRY_COUNT = 3
        val BACKOFF_DELAYS_MS = longArrayOf(1_000L, 5_000L, 25_000L)

        /** リトライ回数を追跡する AMQP ヘッダ。 */
        const val RETRY_COUNT_HEADER = "x-openpos-retry-count"

        /**
         * これを超えるペイロードは再送せず永久失敗として記録する（フェイルセーフ）。
         * RabbitMQ の max_message_size（既定 16MiB）を publish 時に踏み抜くと
         * チャネル例外で readiness が落ちるため、十分小さい値で手前で止める。
         */
        const val MAX_RETRY_PAYLOAD_BYTES = 1_048_576

        /** 元メッセージに content_type がない場合の既定値（発行側は application/json を明示している）。 */
        const val DEFAULT_CONTENT_TYPE = "application/json"
    }

    @Incoming("dlq-inventory-sale-completed")
    fun onDlqSaleCompleted(message: IncomingRabbitMQMessage<*>): CompletionStage<Void> =
        try {
            handleDeadLetter(message, "inventory.sale-completed") { retryMessage ->
                saleCompletedRetryEmitter.send(retryMessage)
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process DLQ sale-completed message, sending nack")
            message.nack(e)
        }

    @Incoming("dlq-inventory-sale-voided")
    fun onDlqSaleVoided(message: IncomingRabbitMQMessage<*>): CompletionStage<Void> =
        try {
            handleDeadLetter(message, "inventory.sale-voided") { retryMessage ->
                saleVoidedRetryEmitter.send(retryMessage)
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process DLQ sale-voided message, sending nack")
            message.nack(e)
        }

    /**
     * サポートする payload 型を文字列化する。サポート外の型は null を返し、呼び出し側で
     * 永久失敗として扱う。ObjectMapper による汎用シリアライズは「元のバイト列と異なる表現」を
     * 生み、リトライごとの再エンコード肥大化（#1259）の温床になるため行わない。
     */
    private fun payloadAsStringOrNull(payload: Any?): String? =
        when (payload) {
            is String -> payload
            is ByteArray -> String(payload, StandardCharsets.UTF_8)
            is JsonObject -> payload.encode()
            else -> null
        }

    private fun handleDeadLetter(
        message: IncomingRabbitMQMessage<*>,
        originalQueue: String,
        republish: (Message<String>) -> Unit,
    ) {
        val incomingMetadata =
            message
                .getMetadata()
                .get(IncomingRabbitMQMetadata::class.java)
                .orElse(null)
        val retryCount = retryCountOf(incomingMetadata)
        val body = payloadAsStringOrNull(message.payload)
        val eventId = extractField(body, "eventId")
        val eventType = extractField(body, "eventType")

        when {
            body == null -> {
                logPermanentFailure(
                    originalQueue,
                    eventId,
                    eventType,
                    "unsupported payload type: ${message.payload?.javaClass?.name ?: "null"}",
                )
            }

            body.toByteArray(StandardCharsets.UTF_8).size > MAX_RETRY_PAYLOAD_BYTES -> {
                logPermanentFailure(
                    originalQueue,
                    eventId,
                    eventType,
                    "payload exceeds retry size limit ($MAX_RETRY_PAYLOAD_BYTES bytes)",
                )
            }

            retryCount >= MAX_RETRY_COUNT -> {
                logPermanentFailure(
                    originalQueue,
                    eventId,
                    eventType,
                    "exceeded max retries ($MAX_RETRY_COUNT)",
                )
                log.debugf("PERMANENT FAILURE full payload for eventId=%s: %s", eventId, body)
            }

            else -> {
                val delayMs = BACKOFF_DELAYS_MS[retryCount]
                log.warnf(
                    "DLQ message retry %d/%d for queue=%s, eventId=%s, eventType=%s, delay=%dms",
                    retryCount + 1,
                    MAX_RETRY_COUNT,
                    originalQueue,
                    eventId,
                    eventType,
                    delayMs,
                )

                val contentType =
                    incomingMetadata?.contentType?.orElse(DEFAULT_CONTENT_TYPE) ?: DEFAULT_CONTENT_TYPE
                val outgoingMetadata =
                    OutgoingRabbitMQMetadata
                        .builder()
                        .withExpiration(delayMs.toString())
                        .withContentType(contentType)
                        .withHeader(RETRY_COUNT_HEADER, (retryCount + 1).toLong())
                        .build()
                republish(Message.of(body, Metadata.of(outgoingMetadata)))
            }
        }
    }

    /** AMQP ヘッダからリトライ回数を読む。ヘッダなし（初回 DLQ 到着）は 0。 */
    private fun retryCountOf(metadata: IncomingRabbitMQMetadata?): Int =
        try {
            metadata
                ?.getHeader(RETRY_COUNT_HEADER, Number::class.java)
                ?.map { it.toInt() }
                ?.orElse(0) ?: 0
        } catch (e: ClassCastException) {
            log.warnf("Unreadable %s header (%s); treating as 0", RETRY_COUNT_HEADER, e.message)
            0
        }

    private fun logPermanentFailure(
        originalQueue: String,
        eventId: String,
        eventType: String,
        reason: String,
    ) {
        log.errorf(
            "PERMANENT FAILURE: DLQ message dropped for queue=%s, eventId=%s, eventType=%s, reason=%s",
            originalQueue,
            eventId,
            eventType,
            reason,
        )
    }

    /** ログ用途の best-effort 抽出。parse 失敗はリトライ判断に影響しない。 */
    @Suppress("UNCHECKED_CAST")
    private fun extractField(
        message: String?,
        field: String,
    ): String =
        try {
            if (message == null) {
                "unknown"
            } else {
                val map = objectMapper.readValue(message, Map::class.java) as Map<String, Any>
                map[field]?.toString() ?: "unknown"
            }
        } catch (e: Exception) {
            "unknown"
        }
}
