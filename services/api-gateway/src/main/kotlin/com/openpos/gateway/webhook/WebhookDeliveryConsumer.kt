package com.openpos.gateway.webhook

import io.quarkus.scheduler.Scheduled
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * Webhook 配信コンシューマー (#908)。
 * Redis ポーリングで PENDING/RETRYING 配信を非同期処理する。
 */
@ApplicationScoped
class WebhookDeliveryConsumer {
    @Inject
    lateinit var webhookStore: WebhookStore

    @Inject
    lateinit var deliveryService: WebhookDeliveryService

    companion object {
        private val LOG = Logger.getLogger(WebhookDeliveryConsumer::class.java)
    }

    /**
     * PENDING 状態の配信を 5 秒間隔でポーリングして HTTP 配信を試行する。
     */
    @Scheduled(every = "5s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    fun processPendingDeliveries() {
        val pending = webhookStore.findPendingDeliveries()
        if (pending.isEmpty()) return

        LOG.debugf("Processing %d pending webhook deliveries", pending.size)
        for (delivery in pending) {
            val webhook = webhookStore.findById(delivery.webhookId)
            if (webhook == null) {
                LOG.warnf("Webhook %s not found for delivery %s, marking as failed", delivery.webhookId, delivery.id)
                webhookStore.updateDelivery(
                    delivery.copy(
                        status = DeliveryStatus.FAILED,
                        lastError = "Webhook registration not found",
                        completedAt = java.time.Instant.now(),
                    ),
                )
                continue
            }
            if (!webhook.isActive) {
                webhookStore.updateDelivery(
                    delivery.copy(
                        status = DeliveryStatus.FAILED,
                        lastError = "Webhook is inactive",
                        completedAt = java.time.Instant.now(),
                    ),
                )
                continue
            }
            try {
                deliveryService.attemptDelivery(webhook, delivery)
            } catch (e: Exception) {
                LOG.warnf("Failed to process delivery %s: %s", delivery.id, e.message)
            }
        }
    }

    /**
     * RETRYING 状態の配信を 30 秒間隔でリトライする。
     */
    @Scheduled(every = "30s", concurrentExecution = Scheduled.ConcurrentExecution.SKIP)
    fun processRetries() {
        try {
            val retried = deliveryService.retryPendingDeliveries()
            if (retried > 0) {
                LOG.debugf("Retried %d webhook deliveries", retried)
            }
        } catch (e: Exception) {
            LOG.warnf("Failed to process webhook retries: %s", e.message)
        }
    }
}
