package com.openpos.gateway.webhook

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Webhook 登録エンティティ。
 * テナントごとの Webhook URL とイベントタイプのマッピングを保持する。
 */
data class WebhookRegistration(
    val id: UUID = UUID.randomUUID(),
    val organizationId: UUID,
    val url: String,
    val events: List<String>,
    val secret: String,
    val isActive: Boolean = true,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
)

/**
 * Webhook 配信記録。
 * 各配信の成否・レスポンスコード・リトライ回数を記録する。
 */
data class WebhookDelivery(
    val id: UUID = UUID.randomUUID(),
    val webhookId: UUID,
    val eventType: String,
    val payload: String,
    val status: DeliveryStatus,
    val httpStatusCode: Int? = null,
    val attemptCount: Int = 0,
    val maxRetries: Int = MAX_RETRIES,
    val nextRetryAt: Instant? = null,
    val lastError: String? = null,
    val createdAt: Instant = Instant.now(),
    val completedAt: Instant? = null,
) {
    companion object {
        /** 最大リトライ回数 */
        const val MAX_RETRIES = 5
    }
}

/** 配信ステータス */
enum class DeliveryStatus {
    /** 配信待ち */
    PENDING,

    /** 配信成功 */
    SUCCESS,

    /** リトライ中（次回配信待ち） */
    RETRYING,

    /** 全リトライ失敗 */
    FAILED,
}

/**
 * Webhook 登録のインメモリストア。
 * テナント分離された Webhook 登録を管理する。
 */
@jakarta.enterprise.context.ApplicationScoped
class WebhookStore {
    private val registrations = ConcurrentHashMap<UUID, WebhookRegistration>()
    private val deliveries = ConcurrentHashMap<UUID, WebhookDelivery>()

    fun register(registration: WebhookRegistration): WebhookRegistration {
        registrations[registration.id] = registration
        return registration
    }

    fun findById(id: UUID): WebhookRegistration? = registrations[id]

    fun findByOrganization(organizationId: UUID): List<WebhookRegistration> =
        registrations.values.filter { it.organizationId == organizationId }

    fun findActiveByEvent(
        organizationId: UUID,
        eventType: String,
    ): List<WebhookRegistration> =
        registrations.values.filter {
            it.organizationId == organizationId && it.isActive && it.events.contains(eventType)
        }

    fun update(registration: WebhookRegistration): WebhookRegistration {
        registrations[registration.id] = registration
        return registration
    }

    fun delete(id: UUID): Boolean = registrations.remove(id) != null

    fun recordDelivery(delivery: WebhookDelivery): WebhookDelivery {
        deliveries[delivery.id] = delivery
        return delivery
    }

    fun updateDelivery(delivery: WebhookDelivery): WebhookDelivery {
        deliveries[delivery.id] = delivery
        return delivery
    }

    fun findDeliveries(webhookId: UUID): List<WebhookDelivery> =
        deliveries.values
            .filter { it.webhookId == webhookId }
            .sortedByDescending { it.createdAt }

    fun findPendingRetries(): List<WebhookDelivery> {
        val now = Instant.now()
        return deliveries.values.filter {
            it.status == DeliveryStatus.RETRYING &&
                it.nextRetryAt != null &&
                !it.nextRetryAt.isAfter(now)
        }
    }

    /** テスト用: 全データクリア */
    fun clear() {
        registrations.clear()
        deliveries.clear()
    }
}
