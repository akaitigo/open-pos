package com.openpos.product.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
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
    }

    @Nested
    inner class InvalidatePattern {
        @Test
        fun `パターンに一致するキーが全て削除される`() {
            // Arrange
            val matchedKeys = listOf("openpos:product-service:product:1", "openpos:product-service:product:2")
            whenever(keyCommands.keys("openpos:product-service:product:*")).thenReturn(matchedKeys)

            // Act
            cacheService.invalidatePattern("openpos:product-service:product:*")

            // Assert
            verify(keyCommands).keys("openpos:product-service:product:*")
            verify(keyCommands).del("openpos:product-service:product:1", "openpos:product-service:product:2")
        }

        @Test
        fun `パターンに一致するキーがない場合はdelが呼ばれない`() {
            // Arrange
            whenever(keyCommands.keys("openpos:product-service:nonexistent:*")).thenReturn(emptyList())

            // Act
            cacheService.invalidatePattern("openpos:product-service:nonexistent:*")

            // Assert
            verify(keyCommands).keys("openpos:product-service:nonexistent:*")
            verify(keyCommands, never()).del(any<String>())
        }
    }

    @Nested
    inner class KeyGeneration {
        @Test
        fun `productKeyは正しい形式のキーを生成する`() {
            assertEquals("openpos:product-service:product:abc-123", cacheService.productKey("abc-123"))
        }

        @Test
        fun `productBarcodeKeyは正しい形式のキーを生成する`() {
            assertEquals("openpos:product-service:product:barcode:4901234567890", cacheService.productBarcodeKey("4901234567890"))
        }

        @Test
        fun `categoryKeyは正しい形式のキーを生成する`() {
            assertEquals("openpos:product-service:category:cat-001", cacheService.categoryKey("cat-001"))
        }

        @Test
        fun `categoryListKeyはparentIdありの場合の正しいキーを生成する`() {
            assertEquals("openpos:product-service:category:list:parent-id", cacheService.categoryListKey("parent-id"))
        }

        @Test
        fun `categoryListKeyはparentIdなしの場合rootキーを生成する`() {
            assertEquals("openpos:product-service:category:list:root", cacheService.categoryListKey(null))
        }
    }

    @Nested
    inner class InvalidateProduct {
        @Test
        fun `商品IDとバーコードの両方のキーが削除される`() {
            // Arrange & Act
            cacheService.invalidateProduct("product-123", "4901234567890")

            // Assert
            verify(keyCommands).del(
                "openpos:product-service:product:product-123",
                "openpos:product-service:product:barcode:4901234567890",
            )
        }

        @Test
        fun `バーコードがnullの場合は商品IDキーのみ削除される`() {
            // Arrange & Act
            cacheService.invalidateProduct("product-123", null)

            // Assert
            verify(keyCommands).del("openpos:product-service:product:product-123")
        }
    }

    @Nested
    inner class InvalidateCategory {
        @Test
        fun `カテゴリIDキーとリストパターンが削除される`() {
            // Arrange
            whenever(keyCommands.keys("openpos:product-service:category:list:*"))
                .thenReturn(listOf("openpos:product-service:category:list:root"))

            // Act
            cacheService.invalidateCategory("cat-001")

            // Assert
            verify(keyCommands).del("openpos:product-service:category:cat-001")
            verify(keyCommands).keys("openpos:product-service:category:list:*")
        }
    }
}
