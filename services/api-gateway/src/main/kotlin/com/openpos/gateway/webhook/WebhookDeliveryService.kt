package com.openpos.gateway.webhook

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Webhook 配信サービス (#410)。
 * 指数バックオフによるリトライと配信トラッキングを提供する。
 *
 * リトライ間隔: 30s, 120s, 480s, 1920s, 7680s（約2時間）
 */
@ApplicationScoped
class WebhookDeliveryService {
    @Inject
    lateinit var webhookStore: WebhookStore

    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    /**
     * 指定イベントタイプに該当する全 Webhook に配信する。
     *
     * @param organizationId テナントID
     * @param eventType イベントタイプ（例: "sale.completed", "product.created"）
     * @param payload JSON ペイロード
     * @return 作成された配信記録のリスト
     */
    fun deliver(
        organizationId: UUID,
        eventType: String,
        payload: String,
    ): List<WebhookDelivery> {
        val webhooks = webhookStore.findActiveByEvent(organizationId, eventType)
        return webhooks.map { webhook ->
            deliverToWebhook(webhook, eventType, payload)
        }
    }

    /**
     * 単一 Webhook への配信を試行する。
     */
    fun deliverToWebhook(
        webhook: WebhookRegistration,
        eventType: String,
        payload: String,
    ): WebhookDelivery {
        val delivery =
            WebhookDelivery(
                webhookId = webhook.id,
                eventType = eventType,
                payload = payload,
                status = DeliveryStatus.PENDING,
            )
        webhookStore.recordDelivery(delivery)

        return attemptDelivery(webhook, delivery)
    }

    /**
     * リトライ対象の配信を再試行する。
     * スケジューラーから定期的に呼び出されることを想定する。
     */
    fun retryPendingDeliveries(organizationId: UUID? = null): Int {
        val pending = webhookStore.findPendingRetries()
        var retried = 0
        for (delivery in pending) {
            val webhook = webhookStore.findById(delivery.webhookId) ?: continue
            if (organizationId != null && webhook.organizationId != organizationId) continue
            if (!webhook.isActive) {
                webhookStore.updateDelivery(
                    delivery.copy(
                        status = DeliveryStatus.FAILED,
                        lastError = "Webhook is inactive",
                        completedAt = Instant.now(),
                    ),
                )
                continue
            }
            attemptDelivery(webhook, delivery)
            retried++
        }
        return retried
    }

    /**
     * 配信を試行する。成功すれば SUCCESS、失敗すればリトライをスケジュールまたは FAILED にする。
     */
    internal fun attemptDelivery(
        webhook: WebhookRegistration,
        delivery: WebhookDelivery,
    ): WebhookDelivery {
        val attempt = delivery.attemptCount + 1

        return try {
            val signature = computeSignature(delivery.payload, webhook.secret)
            val request =
                HttpRequest
                    .newBuilder()
                    .uri(URI.create(webhook.url))
                    .timeout(Duration.ofSeconds(DELIVERY_TIMEOUT_SECONDS))
                    .header("Content-Type", "application/json")
                    .header("X-Webhook-Signature", signature)
                    .header("X-Webhook-Event", delivery.eventType)
                    .header("X-Webhook-Delivery-Id", delivery.id.toString())
                    .POST(HttpRequest.BodyPublishers.ofString(delivery.payload))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() in 200..299) {
                val updated =
                    delivery.copy(
                        status = DeliveryStatus.SUCCESS,
                        httpStatusCode = response.statusCode(),
                        attemptCount = attempt,
                        completedAt = Instant.now(),
                    )
                webhookStore.updateDelivery(updated)
                updated
            } else {
                handleFailure(
                    delivery,
                    attempt,
                    "HTTP ${response.statusCode()}",
                    response.statusCode(),
                )
            }
        } catch (e: Exception) {
            LOG.warnf("Webhook delivery failed for %s: %s", webhook.url, e.message)
            handleFailure(delivery, attempt, e.message ?: "Unknown error", null)
        }
    }

    /**
     * 配信失敗時の処理。リトライ回数が上限以内ならリトライをスケジュールする。
     */
    private fun handleFailure(
        delivery: WebhookDelivery,
        attempt: Int,
        errorMessage: String,
        httpStatus: Int?,
    ): WebhookDelivery {
        if (attempt >= delivery.maxRetries) {
            val failed =
                delivery.copy(
                    status = DeliveryStatus.FAILED,
                    httpStatusCode = httpStatus,
                    attemptCount = attempt,
                    lastError = errorMessage,
                    completedAt = Instant.now(),
                )
            webhookStore.updateDelivery(failed)
            return failed
        }

        val backoffSeconds = calculateBackoff(attempt)
        val retrying =
            delivery.copy(
                status = DeliveryStatus.RETRYING,
                httpStatusCode = httpStatus,
                attemptCount = attempt,
                lastError = errorMessage,
                nextRetryAt = Instant.now().plusSeconds(backoffSeconds),
            )
        webhookStore.updateDelivery(retrying)
        return retrying
    }

    companion object {
        private val LOG = Logger.getLogger(WebhookDeliveryService::class.java)

        /** 配信タイムアウト（秒） */
        const val DELIVERY_TIMEOUT_SECONDS = 10L

        /** ベースバックオフ（秒） */
        const val BASE_BACKOFF_SECONDS = 30L

        /**
         * 指数バックオフを計算する。
         * attempt=1 -> 30s, attempt=2 -> 120s, attempt=3 -> 480s, ...
         */
        fun calculateBackoff(attempt: Int): Long {
            val factor = 1L shl (attempt - 1).coerceAtMost(10)
            return BASE_BACKOFF_SECONDS * factor
        }

        /**
         * HMAC-SHA256 署名を計算する。
         * Webhook受信側はこの署名でペイロードの真正性を検証できる。
         */
        fun computeSignature(
            payload: String,
            secret: String,
        ): String {
            val mac = Mac.getInstance("HmacSHA256")
            mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA256"))
            val hash = mac.doFinal(payload.toByteArray(Charsets.UTF_8))
            return "sha256=" + hash.joinToString("") { "%02x".format(it) }
        }
    }
}
