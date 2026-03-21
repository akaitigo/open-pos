package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.util.concurrent.CompletionStage

/**
 * inventory-service の Dead Letter Queue コンシューマー。
 * DLQ に到着したメッセージを監視し、リトライ可能な場合は元のキューに再送する。
 * 最大 3 回のリトライ（指数バックオフ: 1s, 5s, 25s）を試み、
 * それを超えた場合は永久失敗としてログに記録する。
 * 明示的に ack/nack を行う。
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
    }

    @Incoming("dlq-inventory-sale-completed")
    fun onDlqSaleCompleted(message: IncomingRabbitMQMessage<String>): CompletionStage<Void> =
        try {
            val body = message.payload
            handleDeadLetter(body, "inventory.sale-completed") { retryMessage ->
                saleCompletedRetryEmitter.send(retryMessage)
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process DLQ sale-completed message, sending nack")
            message.nack(e)
        }

    @Incoming("dlq-inventory-sale-voided")
    fun onDlqSaleVoided(message: IncomingRabbitMQMessage<String>): CompletionStage<Void> =
        try {
            val body = message.payload
            handleDeadLetter(body, "inventory.sale-voided") { retryMessage ->
                saleVoidedRetryEmitter.send(retryMessage)
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process DLQ sale-voided message, sending nack")
            message.nack(e)
        }

    private fun handleDeadLetter(
        messageBody: String,
        originalQueue: String,
        republish: (String) -> Unit,
    ) {
        val retryCount = extractRetryCount(messageBody)
        val eventId = extractField(messageBody, "eventId")
        val eventType = extractField(messageBody, "eventType")

        if (retryCount < MAX_RETRY_COUNT) {
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

            Thread.sleep(delayMs)

            val updatedMessage = incrementRetryCount(messageBody, retryCount)
            republish(updatedMessage)
        } else {
            log.errorf(
                "PERMANENT FAILURE: DLQ message exceeded max retries (%d) for queue=%s, eventId=%s, eventType=%s",
                MAX_RETRY_COUNT,
                originalQueue,
                eventId,
                eventType,
            )
            log.debugf("PERMANENT FAILURE full payload for eventId=%s: %s", eventId, messageBody)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractField(
        message: String,
        field: String,
    ): String =
        try {
            val map = objectMapper.readValue(message, Map::class.java) as Map<String, Any>
            map[field]?.toString() ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }

    @Suppress("UNCHECKED_CAST")
    private fun extractRetryCount(message: String): Int =
        try {
            val map = objectMapper.readValue(message, Map::class.java) as Map<String, Any>
            (map["retryCount"] as? Number)?.toInt() ?: 0
        } catch (e: Exception) {
            0
        }

    @Suppress("UNCHECKED_CAST")
    private fun incrementRetryCount(
        message: String,
        currentRetryCount: Int,
    ): String =
        try {
            val map = objectMapper.readValue(message, LinkedHashMap::class.java) as MutableMap<String, Any>
            map["retryCount"] = currentRetryCount + 1
            objectMapper.writeValueAsString(map)
        } catch (e: Exception) {
            log.warnf("Failed to increment retry count: %s", e.message)
            message
        }
}
