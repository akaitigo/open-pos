package com.openpos.gateway.config

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RateLimitFilterTest {
    private val redisDataSource: RedisDataSource = mock()
    private val valueCommands: ValueCommands<String, Long> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val filter =
        RateLimitFilter().also {
            val redisField = RateLimitFilter::class.java.getDeclaredField("redis")
            redisField.isAccessible = true
            redisField.set(it, redisDataSource)
        }

    @BeforeEach
    fun setUp() {
        whenever(redisDataSource.value(Long::class.java)).thenReturn(valueCommands)
        whenever(redisDataSource.key()).thenReturn(keyCommands)
    }

    @Nested
    inner class TryIncrementTest {
        @Test
        fun `最初のリクエストでINCRとEXPIREが呼ばれる`() {
            // Arrange
            val key = "ratelimit:tenant1:12345"
            whenever(valueCommands.incr(key)).thenReturn(1L)

            // Act
            val count = filter.tryIncrement(key)

            // Assert
            assertEquals(1L, count)
            verify(valueCommands).incr(key)
            verify(keyCommands).expire(key, 60L)
        }

        @Test
        fun `2回目以降のリクエストではEXPIREが呼ばれない`() {
            // Arrange
            val key = "ratelimit:tenant1:12345"
            whenever(valueCommands.incr(key)).thenReturn(5L)

            // Act
            val count = filter.tryIncrement(key)

            // Assert
            assertEquals(5L, count)
            verify(valueCommands).incr(key)
            verify(keyCommands, never()).expire(any<String>(), any<Long>())
        }

        @Test
        fun `Redis障害時は-1を返す（fail-open）`() {
            // Arrange
            val key = "ratelimit:tenant1:12345"
            whenever(valueCommands.incr(key)).thenThrow(RuntimeException("Connection refused"))

            // Act
            val count = filter.tryIncrement(key)

            // Assert
            assertEquals(-1L, count)
        }
    }

    @Nested
    inner class CurrentMinuteKeyTest {
        @Test
        fun `currentMinuteKeyは空でない文字列を返す`() {
            // Act
            val key = filter.currentMinuteKey()

            // Assert
            assertTrue(key.isNotBlank())
            assertTrue(key.toLongOrNull() != null, "minuteKey should be numeric")
        }
    }

    @Nested
    inner class NextMinuteEpochSecondTest {
        @Test
        fun `nextMinuteEpochSecondは未来のタイムスタンプを返す`() {
            // Act
            val resetAt = filter.nextMinuteEpochSecond()

            // Assert
            assertTrue(resetAt > System.currentTimeMillis() / 1000)
        }

        @Test
        fun `nextMinuteEpochSecondは60秒以内の値を返す`() {
            // Act
            val resetAt = filter.nextMinuteEpochSecond()
            val now = System.currentTimeMillis() / 1000

            // Assert
            assertTrue(resetAt - now <= 60)
            assertTrue(resetAt - now >= 0)
        }
    }
}
