package com.openpos.product.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.keys.KeyScanCursor
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ProductCacheServiceTest {
    private val redisDataSource: RedisDataSource = mock()
    private val valueCommands: ValueCommands<String, String> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val cacheService =
        ProductCacheService().also {
            val field = ProductCacheService::class.java.getDeclaredField("redis")
            field.isAccessible = true
            field.set(it, redisDataSource)
        }

    @BeforeEach
    fun setUp() {
        whenever(redisDataSource.value(String::class.java)).thenReturn(valueCommands)
        whenever(redisDataSource.key()).thenReturn(keyCommands)
    }

    @Nested
    inner class Get {
        @Test
        fun `キャッシュヒット時は値を返す`() {
            // Arrange
            val key = "openpos:product-service:product:123"
            whenever(valueCommands.get(key)).thenReturn("cached-product")

            // Act
            val result = cacheService.get(key)

            // Assert
            assertEquals("cached-product", result)
        }

        @Test
        fun `キャッシュミス時はnullを返す`() {
            // Arrange
            val key = "openpos:product-service:product:999"
            whenever(valueCommands.get(key)).thenReturn(null)

            // Act
            val result = cacheService.get(key)

            // Assert
            assertNull(result)
        }

        @Test
        fun `Redis例外時はnullを返す`() {
            // Arrange
            val key = "openpos:product-service:product:123"
            whenever(valueCommands.get(key)).thenThrow(RuntimeException("Connection lost"))

            // Act
            val result = cacheService.get(key)

            // Assert
            assertNull(result)
        }
    }

    @Nested
    inner class Set {
        @Test
        fun `デフォルトTTL（3600秒）でsetexが呼ばれる`() {
            // Arrange & Act
            cacheService.set("openpos:product-service:product:123", "value")

            // Assert
            verify(valueCommands).setex("openpos:product-service:product:123", 3600L, "value")
        }

        @Test
        fun `カスタムTTLでsetexが呼ばれる`() {
            // Arrange & Act
            cacheService.set("openpos:product-service:product:123", "value", ttlSeconds = 120L)

            // Assert
            verify(valueCommands).setex("openpos:product-service:product:123", 120L, "value")
        }

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            // Arrange
            whenever(valueCommands.setex(any(), any(), any())).thenThrow(RuntimeException("Connection lost"))

            // Act (should not throw)
            cacheService.set("openpos:product-service:product:123", "value")
        }
    }

    @Nested
    inner class Invalidate {
        @Test
        fun `単一キーでdelが呼ばれる`() {
            // Arrange & Act
            cacheService.invalidate("openpos:product-service:product:123")

            // Assert
            verify(keyCommands).del("openpos:product-service:product:123")
        }

        @Test
        fun `複数キーでdelが全キーで呼ばれる`() {
            // Arrange & Act
            cacheService.invalidate("openpos:product-service:product:1", "openpos:product-service:product:2")

            // Assert
            verify(keyCommands).del("openpos:product-service:product:1", "openpos:product-service:product:2")
        }

        @Test
        fun `空のキーではdelが呼ばれない`() {
            // Arrange & Act
            cacheService.invalidate()

            // Assert
            verify(keyCommands, never()).del(any<String>())
        }

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            whenever(keyCommands.del(any<String>())).thenThrow(RuntimeException("Connection lost"))
            cacheService.invalidate("openpos:product-service:product:123")
        }
    }

    @Nested
    inner class InvalidatePattern {
        @Test
        fun `SCANでキーを取得して削除する`() {
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(true, false)
            whenever(cursor.next()).thenReturn(setOf("key1", "key2"))

            cacheService.invalidatePattern("openpos:product-service:product:*")

            verify(keyCommands).del("key1", "key2")
        }

        @Test
        fun `SCANでキーが空の場合はdelが呼ばれない`() {
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(false)

            cacheService.invalidatePattern("openpos:product-service:product:*")

            verify(keyCommands, never()).del(any<String>())
        }

        @Test
        fun `Redis例外時はスローせずログ出力する`() {
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection lost"))
            cacheService.invalidatePattern("openpos:product-service:product:*")
        }
    }

    @Nested
    inner class GetOrLoad {
        @Test
        fun `キャッシュヒット時はloaderを呼ばずにキャッシュ値を返す`() {
            val key = "openpos:product-service:product:123"
            whenever(valueCommands.get(key)).thenReturn("cached-value")

            val result = cacheService.getOrLoad(key) { "loaded-value" }

            assertEquals("cached-value", result)
            verify(valueCommands, never()).setnx(any(), any())
        }

        @Test
        fun `キャッシュミスでロック取得成功時はloaderを呼んでキャッシュに設定する`() {
            val key = "openpos:product-service:product:456"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)

            val result = cacheService.getOrLoad(key) { "loaded-value" }

            assertEquals("loaded-value", result)
            verify(valueCommands).setex(key, 3600L, "loaded-value")
            verify(keyCommands).del(lockKey)
        }

        @Test
        fun `loaderがnullを返す場合はキャッシュに設定せずnullを返す`() {
            val key = "openpos:product-service:product:789"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)

            val result = cacheService.getOrLoad(key) { null }

            assertNull(result)
            verify(valueCommands, never()).setex(any(), any(), any())
            verify(keyCommands).del(lockKey)
        }

        @Test
        fun `ロック取得失敗時はリトライ後にキャッシュ値を返す`() {
            val key = "openpos:product-service:product:retry"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key))
                .thenReturn(null)
                .thenReturn("eventually-cached")
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(false)

            val result = cacheService.getOrLoad(key) { "fallback" }

            assertEquals("eventually-cached", result)
        }

        @Test
        fun `SETNX例外時はフォールバックでloaderを呼ぶ`() {
            val key = "openpos:product-service:product:err"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(any(), any())).thenThrow(RuntimeException("Redis down"))

            val result = cacheService.getOrLoad(key) { "fallback-value" }

            assertEquals("fallback-value", result)
        }

        @Test
        fun `ロック解放失敗時でもloaderの結果は返される`() {
            val key = "openpos:product-service:product:lock-del-fail"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)
            whenever(keyCommands.del(lockKey)).thenThrow(RuntimeException("Redis down"))

            val result = cacheService.getOrLoad(key) { "loaded-despite-error" }

            assertEquals("loaded-despite-error", result)
        }

        @Test
        fun `ロック取得失敗でリトライ全て失敗した場合はloaderを直接呼ぶ`() {
            val key = "openpos:product-service:product:timeout"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(false)

            val result = cacheService.getOrLoad(key) { "direct-load" }

            assertEquals("direct-load", result)
        }

        @Test
        fun `リトライ中にInterruptedException発生時はloaderを直接呼ぶ`() {
            val key = "openpos:product-service:product:interrupt"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(false)

            Thread.currentThread().interrupt()
            val result = cacheService.getOrLoad(key) { "interrupted-fallback" }

            assertEquals("interrupted-fallback", result)
            // interrupted フラグがクリアされていることを確認
            Thread.interrupted()
        }

        @Test
        fun `カスタムTTLでgetOrLoadが動作する`() {
            val key = "openpos:product-service:product:custom-ttl"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)

            val result = cacheService.getOrLoad(key, ttlSeconds = 120L) { "loaded" }

            assertEquals("loaded", result)
            verify(valueCommands).setex(key, 120L, "loaded")
        }
    }

    @Nested
    inner class KeyGeneration {
        @Test
        fun `productKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals("openpos:product-service:org-1:product:abc-123", cacheService.productKey("org-1", "abc-123"))
        }

        @Test
        fun `productBarcodeKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals(
                "openpos:product-service:org-1:product:barcode:4901234567890",
                cacheService.productBarcodeKey("org-1", "4901234567890"),
            )
        }

        @Test
        fun `categoryKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals("openpos:product-service:org-1:category:cat-001", cacheService.categoryKey("org-1", "cat-001"))
        }

        @Test
        fun `categoryListKeyはparentIdありの場合orgIdを含む正しいキーを生成する`() {
            assertEquals(
                "openpos:product-service:org-1:category:list:parent-id",
                cacheService.categoryListKey("org-1", "parent-id"),
            )
        }

        @Test
        fun `categoryListKeyはparentIdなしの場合orgIdを含むrootキーを生成する`() {
            assertEquals("openpos:product-service:org-1:category:list:root", cacheService.categoryListKey("org-1", null))
        }
    }

    @Nested
    inner class InvalidateProduct {
        @Test
        fun `商品IDとバーコードの両方のキーが削除される`() {
            // Arrange & Act
            cacheService.invalidateProduct("org-1", "product-123", "4901234567890")

            // Assert
            verify(keyCommands).del(
                "openpos:product-service:org-1:product:product-123",
                "openpos:product-service:org-1:product:barcode:4901234567890",
            )
        }

        @Test
        fun `バーコードがnullの場合は商品IDキーのみ削除される`() {
            // Arrange & Act
            cacheService.invalidateProduct("org-1", "product-123", null)

            // Assert
            verify(keyCommands).del("openpos:product-service:org-1:product:product-123")
        }
    }

    @Nested
    inner class InvalidateCategory {
        @Test
        fun `カテゴリIDキーが削除される`() {
            // Arrange: scan が例外を投げてもエラーにならない
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection lost"))

            // Act
            cacheService.invalidateCategory("org-1", "cat-001")

            // Assert
            verify(keyCommands).del("openpos:product-service:org-1:category:cat-001")
        }
    }

    @Nested
    inner class InvalidateAllCategoryLists {
        @Test
        fun `全カテゴリリストキャッシュのパターン削除が呼ばれる`() {
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(true, false)
            whenever(cursor.next()).thenReturn(setOf("openpos:product-service:org-1:category:list:root"))

            cacheService.invalidateAllCategoryLists("org-1")

            verify(keyCommands).del("openpos:product-service:org-1:category:list:root")
        }
    }
}
