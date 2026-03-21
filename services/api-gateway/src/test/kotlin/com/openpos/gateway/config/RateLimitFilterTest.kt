package com.openpos.gateway.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class RateLimitFilterTest {
    @Nested
    inner class RateBucketTest {
        @Test
        fun `リミット内のリクエストは正の残数を返す`() {
            // Arrange
            val bucket = RateLimitFilter.RateBucket()

            // Act
            val remaining = bucket.tryConsume(10)

            // Assert
            assertEquals(9, remaining)
        }

        @Test
        fun `リミットちょうどのリクエストで残数0を返す`() {
            // Arrange
            val bucket = RateLimitFilter.RateBucket()

            // Act
            var remaining = 0
            repeat(10) {
                remaining = bucket.tryConsume(10)
            }

            // Assert
            assertEquals(0, remaining)
        }

        @Test
        fun `リミット超過のリクエストは負の残数を返す`() {
            // Arrange
            val bucket = RateLimitFilter.RateBucket()

            // Act
            var remaining = 0
            repeat(11) {
                remaining = bucket.tryConsume(10)
            }

            // Assert
            assertTrue(remaining < 0)
        }

        @Test
        fun `windowResetEpochSecondは未来のタイムスタンプを返す`() {
            // Arrange
            val bucket = RateLimitFilter.RateBucket()
            bucket.tryConsume(10)

            // Act
            val resetAt = bucket.windowResetEpochSecond()

            // Assert
            assertTrue(resetAt > System.currentTimeMillis() / 1000)
        }

        @Test
        fun `retryAfterSecondsは正の値を返す`() {
            // Arrange
            val bucket = RateLimitFilter.RateBucket()
            bucket.tryConsume(1)

            // Act
            val retryAfter = bucket.retryAfterSeconds()

            // Assert
            assertTrue(retryAfter >= 1)
        }
    }
}
