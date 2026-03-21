package com.openpos.gateway.cache

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

class RedisCacheServiceTest {
    private val redisDataSource: RedisDataSource = mock()
    private val valueCommands: ValueCommands<String, String> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val redisCacheService =
        RedisCacheService().also {
            val field = RedisCacheService::class.java.getDeclaredField("redis")
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
            whenever(valueCommands.get("openpos:product:123")).thenReturn("cached-value")
            val result = redisCacheService.get("openpos:product:123")
            assertEquals("cached-value", result)
        }

        @Test
        fun `キャッシュミス時はnullを返す`() {
            whenever(valueCommands.get("openpos:product:999")).thenReturn(null)
            val result = redisCacheService.get("openpos:product:999")
            assertNull(result)
        }

        @Test
        fun `Redis例外時はnullを返す`() {
            whenever(valueCommands.get("openpos:product:123")).thenThrow(RuntimeException("Connection refused"))
            val result = redisCacheService.get("openpos:product:123")
            assertNull(result)
        }
    }

    @Nested
    inner class Set {
        @Test
        fun `デフォルトTTLでsetexが呼ばれる`() {
            redisCacheService.set("openpos:product:123", "value")
            verify(valueCommands).setex("openpos:product:123", 3600L, "value")
        }

        @Test
        fun `カスタムTTLでsetexが呼ばれる`() {
            redisCacheService.set("openpos:product:123", "value", ttlSeconds = 600L)
            verify(valueCommands).setex("openpos:product:123", 600L, "value")
        }

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            whenever(valueCommands.setex(eq("openpos:product:123"), eq(3600L), eq("value")))
                .thenThrow(RuntimeException("Connection refused"))
            redisCacheService.set("openpos:product:123", "value")
        }
    }

    @Nested
    inner class Invalidate {
        @Test
        fun `単一キーでdelが呼ばれる`() {
            redisCacheService.invalidate("openpos:product:123")
            verify(keyCommands).del("openpos:product:123")
        }

        @Test
        fun `複数キーでdelが全キーで呼ばれる`() {
            redisCacheService.invalidate("openpos:product:1", "openpos:product:2", "openpos:product:3")
            verify(keyCommands).del("openpos:product:1", "openpos:product:2", "openpos:product:3")
        }

        @Test
        fun `空のキーではdelが呼ばれない`() {
            redisCacheService.invalidate()
            verify(keyCommands, never()).del(any<String>())
        }
    }

    @Nested
    inner class InvalidatePattern {
        @Test
        fun `Redis例外時はスローせずログ出力する`() {
            // Arrange: scan が例外を投げる場合
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection refused"))

            // Act (should not throw)
            redisCacheService.invalidatePattern("openpos:product:*")
        }
    }
}
