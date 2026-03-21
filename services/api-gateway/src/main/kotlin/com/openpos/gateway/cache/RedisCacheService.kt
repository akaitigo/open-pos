package com.openpos.gateway.cache

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * api-gateway の Redis キャッシュサービス。
 * cache-aside パターンで API レスポンスをキャッシュする。
 * Redis 障害時はキャッシュをスキップしてバックエンドへフォールバックする。
 */
@ApplicationScoped
class RedisCacheService {
    @Inject
    lateinit var redis: RedisDataSource

    private val log = Logger.getLogger(RedisCacheService::class.java)

    companion object {
        private const val DEFAULT_TTL_SECONDS = 3600L
    }

    fun get(key: String): String? =
        try {
            redis.value(String::class.java).get(key)
        } catch (e: Exception) {
            log.warnf("Redis GET failed for key=%s: %s", key, e.message)
            null
        }

    fun set(
        key: String,
        value: String,
        ttlSeconds: Long = DEFAULT_TTL_SECONDS,
    ) {
        try {
            redis.value(String::class.java).setex(key, ttlSeconds, value)
        } catch (e: Exception) {
            log.warnf("Redis SET failed for key=%s: %s", key, e.message)
        }
    }

    fun invalidate(vararg keys: String) {
        if (keys.isEmpty()) return
        try {
            redis.key().del(*keys)
        } catch (e: Exception) {
            log.warnf("Redis DEL failed for keys=%s: %s", keys.joinToString(), e.message)
        }
    }

    fun invalidatePattern(pattern: String) {
        try {
            val matchedKeys = mutableListOf<String>()
            val cursor = redis.key().scan(io.quarkus.redis.datasource.keys.KeyScanArgs().match(pattern).count(100))
            while (cursor.hasNext()) {
                matchedKeys.addAll(cursor.next())
            }
            if (matchedKeys.isNotEmpty()) {
                redis.key().del(*matchedKeys.toTypedArray())
            }
        } catch (e: Exception) {
            log.warnf("Redis invalidatePattern failed for pattern=%s: %s", pattern, e.message)
        }
    }
}
