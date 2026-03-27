package com.openpos.store.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyScanArgs
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * store-service の Redis キャッシュサービス。
 * cache-aside パターンで組織・店舗・端末の検索結果をキャッシュする。
 * キー形式: openpos:store-service:{orgId}:{entity}:{id}
 * TTL: 600 秒（10 分）
 */
@ApplicationScoped
class StoreCacheService {
    @Inject
    lateinit var redis: RedisDataSource

    private val log = Logger.getLogger(StoreCacheService::class.java)

    companion object {
        const val TTL_SECONDS = 600L
        private const val PREFIX = "openpos:store-service"
    }

    // === Generic Operations ===

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
        ttlSeconds: Long = TTL_SECONDS,
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

    // === Organization Keys ===

    fun organizationKey(
        orgId: String,
        id: String,
    ): String = "$PREFIX:$orgId:org:$id"

    // === Store Keys ===

    fun storeKey(
        orgId: String,
        id: String,
    ): String = "$PREFIX:$orgId:store:$id"

    // === Terminal Keys ===

    fun terminalListKey(
        orgId: String,
        storeId: String,
    ): String = "$PREFIX:$orgId:terminal:list:$storeId"

    // === Invalidation Helpers ===

    /**
     * 組織関連のキャッシュを無効化する。
     */
    fun invalidateOrganization(
        orgId: String,
        organizationId: String,
    ) {
        invalidate(organizationKey(orgId, organizationId))
    }

    /**
     * 店舗関連のキャッシュを無効化する。
     */
    fun invalidateStore(
        orgId: String,
        storeId: String,
    ) {
        invalidate(storeKey(orgId, storeId))
    }

    /**
     * 端末リストキャッシュを無効化する。
     */
    fun invalidateTerminalList(
        orgId: String,
        storeId: String,
    ) {
        invalidate(terminalListKey(orgId, storeId))
    }
}
