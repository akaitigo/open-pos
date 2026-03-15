package com.openpos.analytics.event

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger

/**
 * analytics-service の Dead Letter Queue コンシューマー。
 * DLQ に到着したメッセージを監視し、リトライ可能な場合は元のキューに再送する。
 * 最大 3 回のリトライ（指数バックオフ: 1s, 5s, 25s）を試み、
 * それを超えた場合は永久失敗としてログに記録する。
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

    @Incoming("dlq-analytics-sale-completed")
    fun onDlqSaleCompleted(message: String) {
        handleDeadLetter(message, "analytics.sale-completed") { retryMessage ->
            saleCompletedRetryEmitter.send(retryMessage)
        }
    }

    @Incoming("dlq-analytics-sale-voided")
    fun onDlqSaleVoided(message: String) {
        handleDeadLetter(message, "analytics.sale-voided") { retryMessage ->
            saleVoidedRetryEmitter.send(retryMessage)
        }
    }

    private fun handleDeadLetter(
        message: String,
        originalQueue: String,
        republish: (String) -> Unit,
    ) {
        try {
            val retryCount = extractRetryCount(message)

            if (retryCount < MAX_RETRY_COUNT) {
                val delayMs = BACKOFF_DELAYS_MS[retryCount]
                log.warnf(
                    "DLQ message retry %d/%d for queue=%s, delay=%dms",
                    retryCount + 1,
                    MAX_RETRY_COUNT,
                    originalQueue,
                    delayMs,
                )

                Thread.sleep(delayMs)

                val updatedMessage = incrementRetryCount(message, retryCount)
                republish(updatedMessage)
            } else {
                log.errorf(
                    "PERMANENT FAILURE: DLQ message exceeded max retries (%d) for queue=%s. Message: %s",
                    MAX_RETRY_COUNT,
                    originalQueue,
                    message,
                )
            }
        } catch (e: Exception) {
            log.errorf(
                e,
                "PERMANENT FAILURE: Failed to process DLQ message for queue=%s. Message: %s",
                originalQueue,
                message,
            )
        }
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
