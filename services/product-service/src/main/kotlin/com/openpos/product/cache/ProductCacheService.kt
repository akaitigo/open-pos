package com.openpos.product.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyScanArgs
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * product-service の Redis キャッシュサービス。
 * cache-aside パターンで商品・カテゴリの検索結果をキャッシュする。
 * キー形式: openpos:product-service:{orgId}:{entity}:{id}
 * TTL: 3600 秒（1 時間）
 */
@ApplicationScoped
class ProductCacheService {
    @Inject
    lateinit var redis: RedisDataSource

    private val log = Logger.getLogger(ProductCacheService::class.java)

    companion object {
        /** デフォルト TTL: 3600 秒（1 時間） — products, categories 用 */
        const val TTL_SECONDS = 3600L
        private const val PREFIX = "openpos:product-service"
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

    // === Product Keys ===

    fun productKey(
        orgId: String,
        id: String,
    ): String = "$PREFIX:$orgId:product:$id"

    fun productBarcodeKey(
        orgId: String,
        barcode: String,
    ): String = "$PREFIX:$orgId:product:barcode:$barcode"

    // === Category Keys ===

    fun categoryKey(
        orgId: String,
        id: String,
    ): String = "$PREFIX:$orgId:category:$id"

    fun categoryListKey(
        orgId: String,
        parentId: String?,
    ): String = "$PREFIX:$orgId:category:list:${parentId ?: "root"}"

    // === Invalidation Helpers ===

    /**
     * 商品関連のキャッシュを無効化する。
     * ID キーに加え、バーコードキーやリスト系のパターンも削除する。
     */
    fun invalidateProduct(
        orgId: String,
        productId: String,
        barcode: String?,
    ) {
        val keys = mutableListOf(productKey(orgId, productId))
        barcode?.let { keys.add(productBarcodeKey(orgId, it)) }
        invalidate(*keys.toTypedArray())
    }

    /**
     * カテゴリ関連のキャッシュを無効化する。
     * 個別キーに加え、リストキャッシュも削除する。
     */
    fun invalidateCategory(
        orgId: String,
        categoryId: String,
    ) {
        invalidate(categoryKey(orgId, categoryId))
        invalidatePattern("$PREFIX:$orgId:category:list:*")
    }

    /**
     * 全カテゴリリストキャッシュを無効化する。
     */
    fun invalidateAllCategoryLists(orgId: String) {
        invalidatePattern("$PREFIX:$orgId:category:list:*")
    }
}
