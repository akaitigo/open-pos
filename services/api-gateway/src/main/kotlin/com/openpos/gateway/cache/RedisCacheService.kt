package com.openpos.gateway.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyScanArgs
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * api-gateway の Redis キャッシュサービス。
 * cache-aside パターンで API レスポンスをキャッシュする。
 * Redis 障害時はキャッシュをスキップしてバックエンドへフォールバックする。
 *
 * テナント隔離: 全キーに organizationId を含め、テナント間のキャッシュ漏えいを防止する。
 * キーフォーマット: openpos:gateway:{orgId}:{entity}:{id}
 */
@ApplicationScoped
class RedisCacheService {
    @Inject
    lateinit var redis: RedisDataSource

    private val log = Logger.getLogger(RedisCacheService::class.java)

    companion object {
        private const val PREFIX = "openpos:gateway"
        private const val DEFAULT_TTL_SECONDS = 3600L

        /** テナントスコープのキャッシュキーを生成する。 */
        fun tenantKey(
            orgId: String,
            vararg parts: String,
        ): String = "$PREFIX:$orgId:${parts.joinToString(":")}"

        /** テナントスコープのパターン（SCAN 用）を生成する。 */
        fun tenantPattern(
            orgId: String,
            vararg parts: String,
        ): String = "$PREFIX:$orgId:${parts.joinToString(":")}:*"
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

    /**
     * パターンに一致するキーを SCAN で取得して削除する。
     * KEYS コマンドと違い、SCAN はカーソルベースでブロッキングしない。
     */
    fun invalidatePattern(pattern: String) {
        try {
            val allKeys = mutableListOf<String>()
            val cursor = redis.key().scan(KeyScanArgs().match(pattern).count(100))
            while (cursor.hasNext()) {
                allKeys.addAll(cursor.next())
            }
            if (allKeys.isNotEmpty()) {
                redis.key().del(*allKeys.toTypedArray())
            }
        } catch (e: Exception) {
            log.warnf("Redis invalidatePattern failed for pattern=%s: %s", pattern, e.message)
        }
    }
}
