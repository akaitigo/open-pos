package com.openpos.gateway.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.string.StringCommands
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

class RedisCacheServiceTest {
    private val redisDataSource: RedisDataSource = mock()
    private val stringCommands: StringCommands<String, String> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val redisCacheService =
        RedisCacheService().also {
            val field = RedisCacheService::class.java.getDeclaredField("redis")
            field.isAccessible = true
            field.set(it, redisDataSource)
        }

    @BeforeEach
    fun setUp() {
        whenever(redisDataSource.string(String::class.java)).thenReturn(stringCommands)
        whenever(redisDataSource.key()).thenReturn(keyCommands)
    }

    @Nested
    inner class Get {
        @Test
        fun `キャッシュヒット時は値を返す`() {
            // Arrange
            whenever(stringCommands.get("openpos:product:123")).thenReturn("cached-value")

            // Act
            val result = redisCacheService.get("openpos:product:123")

            // Assert
            assertEquals("cached-value", result)
        }

        @Test
        fun `キャッシュミス時はnullを返す`() {
            // Arrange
            whenever(stringCommands.get("openpos:product:999")).thenReturn(null)

            // Act
            val result = redisCacheService.get("openpos:product:999")

            // Assert
            assertNull(result)
        }
    }

    @Nested
    inner class Set {
        @Test
        fun `デフォルトTTLでsetexが呼ばれる`() {
            // Arrange & Act
            redisCacheService.set("openpos:product:123", "value")

            // Assert
            verify(stringCommands).setex("openpos:product:123", 3600L, "value")
        }

        @Test
        fun `カスタムTTLでsetexが呼ばれる`() {
            // Arrange & Act
            redisCacheService.set("openpos:product:123", "value", ttlSeconds = 600L)

            // Assert
            verify(stringCommands).setex("openpos:product:123", 600L, "value")
        }
    }

    @Nested
    inner class Invalidate {
        @Test
        fun `単一キーでdelが呼ばれる`() {
            // Arrange & Act
            redisCacheService.invalidate("openpos:product:123")

            // Assert
            verify(keyCommands).del("openpos:product:123")
        }

        @Test
        fun `複数キーでdelが全キーで呼ばれる`() {
            // Arrange & Act
            redisCacheService.invalidate("openpos:product:1", "openpos:product:2", "openpos:product:3")

            // Assert
            verify(keyCommands).del("openpos:product:1", "openpos:product:2", "openpos:product:3")
        }

        @Test
        fun `空のキーではdelが呼ばれない`() {
            // Arrange & Act
            redisCacheService.invalidate()

            // Assert
            verify(keyCommands, never()).del(any<String>())
        }
    }

    @Nested
    inner class InvalidatePattern {
        @Test
        fun `パターンに一致するキーが全て削除される`() {
            // Arrange
            val matchedKeys = listOf("openpos:product:1", "openpos:product:2")
            whenever(keyCommands.keys("openpos:product:*")).thenReturn(matchedKeys)

            // Act
            redisCacheService.invalidatePattern("openpos:product:*")

            // Assert
            verify(keyCommands).keys("openpos:product:*")
            verify(keyCommands).del("openpos:product:1", "openpos:product:2")
        }

        @Test
        fun `パターンに一致するキーがない場合はdelが呼ばれない`() {
            // Arrange
            whenever(keyCommands.keys("openpos:nonexistent:*")).thenReturn(emptyList())

            // Act
            redisCacheService.invalidatePattern("openpos:nonexistent:*")

            // Assert
            verify(keyCommands).keys("openpos:nonexistent:*")
            verify(keyCommands, never()).del(any<String>())
        }
    }
}
