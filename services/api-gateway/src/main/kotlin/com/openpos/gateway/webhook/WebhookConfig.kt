package com.openpos.gateway.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyScanArgs
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

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

// #908: 非同期配信は Redis ポーリング + @Scheduled で実装済み。
// WebhookDeliveryConsumer が PENDING レコードを定期ポーリングして HTTP 配信を行う。

/**
 * Webhook 登録の Redis ストア (#963)。
 * テナント分離された Webhook 登録と配信記録を Redis に永続化する。
 *
 * キー設計:
 * - 登録: openpos:gateway:webhook:{id} → JSON (TTL なし)
 * - 組織インデックス: openpos:gateway:webhook:org:{orgId} → Set<webhookId>
 * - 配信: openpos:gateway:webhook:delivery:{id} → JSON (7日 TTL)
 * - 配信インデックス: openpos:gateway:webhook:delivery:wh:{webhookId} → Set<deliveryId> (7日 TTL)
 */
@ApplicationScoped
class WebhookStore {
    @Inject
    lateinit var redis: RedisDataSource

    @Inject
    lateinit var objectMapper: ObjectMapper

    companion object {
        private val LOG = Logger.getLogger(WebhookStore::class.java)
        private const val WEBHOOK_PREFIX = "openpos:gateway:webhook:"
        private const val ORG_INDEX_PREFIX = "openpos:gateway:webhook:org:"
        private const val DELIVERY_PREFIX = "openpos:gateway:webhook:delivery:"
        private const val DELIVERY_WH_INDEX_PREFIX = "openpos:gateway:webhook:delivery:wh:"
        private const val DELIVERY_TTL_SECONDS = 7L * 24 * 60 * 60 // 7 days
    }

    fun register(registration: WebhookRegistration): WebhookRegistration {
        try {
            val key = WEBHOOK_PREFIX + registration.id
            val json = objectMapper.writeValueAsString(registration)
            redis.value(String::class.java).set(key, json)
            redis.set(String::class.java).sadd(
                ORG_INDEX_PREFIX + registration.organizationId,
                registration.id.toString(),
            )
        } catch (e: Exception) {
            LOG.errorf("Failed to register webhook %s: %s", registration.id, e.message)
            throw e
        }
        return registration
    }

    fun findById(id: UUID): WebhookRegistration? =
        try {
            val json = redis.value(String::class.java).get(WEBHOOK_PREFIX + id)
            json?.let { objectMapper.readValue(it, WebhookRegistration::class.java) }
        } catch (e: Exception) {
            LOG.warnf("Failed to find webhook %s: %s", id, e.message)
            null
        }

    fun findByOrganization(organizationId: UUID): List<WebhookRegistration> =
        try {
            val ids =
                redis
                    .set(String::class.java)
                    .smembers(ORG_INDEX_PREFIX + organizationId)
            ids.mapNotNull { idStr ->
                try {
                    findById(UUID.fromString(idStr))
                } catch (e: Exception) {
                    LOG.warnf("Failed to parse webhook id %s: %s", idStr, e.message)
                    null
                }
            }
        } catch (e: Exception) {
            LOG.warnf("Failed to find webhooks for org %s: %s", organizationId, e.message)
            emptyList()
        }

    fun findActiveByEvent(
        organizationId: UUID,
        eventType: String,
    ): List<WebhookRegistration> =
        findByOrganization(organizationId).filter {
            it.isActive && it.events.contains(eventType)
        }

    fun update(registration: WebhookRegistration): WebhookRegistration {
        try {
            val key = WEBHOOK_PREFIX + registration.id
            val json = objectMapper.writeValueAsString(registration)
            redis.value(String::class.java).set(key, json)
        } catch (e: Exception) {
            LOG.errorf("Failed to update webhook %s: %s", registration.id, e.message)
            throw e
        }
        return registration
    }

    fun delete(id: UUID): Boolean =
        try {
            val existing = findById(id)
            if (existing != null) {
                redis.key().del(WEBHOOK_PREFIX + id)
                redis.set(String::class.java).srem(
                    ORG_INDEX_PREFIX + existing.organizationId,
                    id.toString(),
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            LOG.errorf("Failed to delete webhook %s: %s", id, e.message)
            false
        }

    fun recordDelivery(delivery: WebhookDelivery): WebhookDelivery {
        try {
            val key = DELIVERY_PREFIX + delivery.id
            val json = objectMapper.writeValueAsString(delivery)
            redis.value(String::class.java).setex(key, DELIVERY_TTL_SECONDS, json)
            val indexKey = DELIVERY_WH_INDEX_PREFIX + delivery.webhookId
            redis.set(String::class.java).sadd(indexKey, delivery.id.toString())
            redis.key().expire(indexKey, DELIVERY_TTL_SECONDS)
        } catch (e: Exception) {
            LOG.errorf("Failed to record delivery %s: %s", delivery.id, e.message)
            throw e
        }
        return delivery
    }

    fun updateDelivery(delivery: WebhookDelivery): WebhookDelivery {
        try {
            val key = DELIVERY_PREFIX + delivery.id
            val json = objectMapper.writeValueAsString(delivery)
            val ttl = redis.key().ttl(key)
            if (ttl > 0) {
                redis.value(String::class.java).setex(key, ttl, json)
            } else {
                redis.value(String::class.java).setex(key, DELIVERY_TTL_SECONDS, json)
            }
        } catch (e: Exception) {
            LOG.errorf("Failed to update delivery %s: %s", delivery.id, e.message)
            throw e
        }
        return delivery
    }

    fun findDeliveries(webhookId: UUID): List<WebhookDelivery> =
        try {
            val ids =
                redis
                    .set(String::class.java)
                    .smembers(DELIVERY_WH_INDEX_PREFIX + webhookId)
            ids
                .mapNotNull { idStr ->
                    try {
                        val json =
                            redis
                                .value(String::class.java)
                                .get(DELIVERY_PREFIX + idStr)
                        json?.let {
                            objectMapper.readValue(it, WebhookDelivery::class.java)
                        }
                    } catch (e: Exception) {
                        LOG.warnf("Failed to read delivery %s: %s", idStr, e.message)
                        null
                    }
                }.sortedByDescending { it.createdAt }
        } catch (e: Exception) {
            LOG.warnf(
                "Failed to find deliveries for webhook %s: %s",
                webhookId,
                e.message,
            )
            emptyList()
        }

    fun findPendingDeliveries(): List<WebhookDelivery> =
        try {
            val allKeys = mutableListOf<String>()
            val cursor =
                redis.key().scan(
                    KeyScanArgs().match(DELIVERY_PREFIX + "*").count(100),
                )
            while (cursor.hasNext()) {
                allKeys.addAll(cursor.next())
            }
            allKeys
                .mapNotNull { key ->
                    try {
                        val json = redis.value(String::class.java).get(key)
                        json?.let {
                            objectMapper.readValue(it, WebhookDelivery::class.java)
                        }
                    } catch (e: Exception) {
                        LOG.warnf("Failed to read delivery from key %s: %s", key, e.message)
                        null
                    }
                }.filter { it.status == DeliveryStatus.PENDING }
        } catch (e: Exception) {
            LOG.warnf("Failed to find pending deliveries: %s", e.message)
            emptyList()
        }

    fun findPendingRetries(): List<WebhookDelivery> {
        val now = Instant.now()
        return try {
            val allKeys = mutableListOf<String>()
            val cursor =
                redis.key().scan(
                    KeyScanArgs().match(DELIVERY_PREFIX + "*").count(100),
                )
            while (cursor.hasNext()) {
                allKeys.addAll(cursor.next())
            }
            allKeys
                .mapNotNull { key ->
                    try {
                        val json = redis.value(String::class.java).get(key)
                        json?.let {
                            objectMapper.readValue(it, WebhookDelivery::class.java)
                        }
                    } catch (e: Exception) {
                        LOG.warnf("Failed to read delivery from key %s: %s", key, e.message)
                        null
                    }
                }.filter {
                    it.status == DeliveryStatus.RETRYING &&
                        it.nextRetryAt != null &&
                        !it.nextRetryAt.isAfter(now)
                }
        } catch (e: Exception) {
            LOG.warnf("Failed to find pending retries: %s", e.message)
            emptyList()
        }
    }

    /** テスト用: 全データクリア */
    fun clear() {
        try {
            val patterns =
                listOf(
                    WEBHOOK_PREFIX + "*",
                    ORG_INDEX_PREFIX + "*",
                    DELIVERY_PREFIX + "*",
                    DELIVERY_WH_INDEX_PREFIX + "*",
                )
            for (pattern in patterns) {
                val keys = mutableListOf<String>()
                val cursor =
                    redis.key().scan(
                        KeyScanArgs().match(pattern).count(100),
                    )
                while (cursor.hasNext()) {
                    keys.addAll(cursor.next())
                }
                if (keys.isNotEmpty()) {
                    redis.key().del(*keys.toTypedArray())
                }
            }
        } catch (e: Exception) {
            LOG.warnf("Failed to clear webhook data: %s", e.message)
        }
    }
}
