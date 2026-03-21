package com.openpos.product.cache

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.jboss.logging.Logger

/**
 * product-service の Redis キャッシュサービス。
 * cache-aside パターンで商品・カテゴリの検索結果をキャッシュする。
 * キー形式: openpos:product-service:{entity}:{id}
 * TTL: 300 秒（5 分）
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

    // === Product Keys ===

    fun productKey(id: String): String = "$PREFIX:product:$id"

    fun productBarcodeKey(barcode: String): String = "$PREFIX:product:barcode:$barcode"

    // === Category Keys ===

    fun categoryKey(id: String): String = "$PREFIX:category:$id"

    fun categoryListKey(parentId: String?): String = "$PREFIX:category:list:${parentId ?: "root"}"

    // === Invalidation Helpers ===

    /**
     * 商品関連のキャッシュを無効化する。
     * ID キーに加え、バーコードキーやリスト系のパターンも削除する。
     */
    fun invalidateProduct(
        productId: String,
        barcode: String?,
    ) {
        val keys = mutableListOf(productKey(productId))
        barcode?.let { keys.add(productBarcodeKey(it)) }
        invalidate(*keys.toTypedArray())
    }

    /**
     * カテゴリ関連のキャッシュを無効化する。
     * 個別キーに加え、リストキャッシュも削除する。
     */
    fun invalidateCategory(categoryId: String) {
        invalidate(categoryKey(categoryId))
        invalidatePattern("$PREFIX:category:list:*")
    }

    /**
     * 全カテゴリリストキャッシュを無効化する。
     */
    fun invalidateAllCategoryLists() {
        invalidatePattern("$PREFIX:category:list:*")
    }
}
