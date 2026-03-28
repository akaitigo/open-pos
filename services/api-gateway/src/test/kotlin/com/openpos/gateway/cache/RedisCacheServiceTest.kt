package com.openpos.gateway.cache

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.keys.KeyScanCursor
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
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

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            whenever(keyCommands.del(any<String>())).thenThrow(RuntimeException("Connection refused"))
            redisCacheService.invalidate("openpos:product:123")
        }
    }

    @Nested
    inner class TenantKeyGeneration {
        private val orgId1 = "11111111-1111-1111-1111-111111111111"
        private val orgId2 = "22222222-2222-2222-2222-222222222222"

        @Test
        fun `tenantKeyはorgIdをプレフィックスに含むキーを生成する`() {
            val key = RedisCacheService.tenantKey(orgId1, "product", "123")
            assertEquals("openpos:gateway:$orgId1:product:123", key)
        }

        @Test
        fun `tenantPatternはorgIdスコープのワイルドカードパターンを生成する`() {
            val pattern = RedisCacheService.tenantPattern(orgId1, "product", "list")
            assertEquals("openpos:gateway:$orgId1:product:list:*", pattern)
        }

        @Test
        fun `異なるorgIdは異なるキーを生成する`() {
            val key1 = RedisCacheService.tenantKey(orgId1, "product", "123")
            val key2 = RedisCacheService.tenantKey(orgId2, "product", "123")
            assertFalse(key1 == key2, "異なるテナントのキーは一致してはならない")
        }

        @Test
        fun `テナントAのパターンはテナントBのキーにマッチしない`() {
            val patternA = RedisCacheService.tenantPattern(orgId1, "product", "list")
            val keyB = RedisCacheService.tenantKey(orgId2, "product", "list", "page1")
            assertFalse(
                keyB.startsWith(patternA.removeSuffix(":*")),
                "テナントAのパターンがテナントBのキーにマッチしてはならない",
            )
        }

        @Test
        fun `テナントAのパターンはテナントAのキーにマッチする`() {
            val patternA = RedisCacheService.tenantPattern(orgId1, "product", "list")
            val keyA = RedisCacheService.tenantKey(orgId1, "product", "list", "page1")
            assertTrue(
                keyA.startsWith(patternA.removeSuffix(":*")),
                "テナントAのパターンはテナントAのキーにマッチすべき",
            )
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

            redisCacheService.invalidatePattern("openpos:product:*")

            verify(keyCommands).del("key1", "key2")
        }

        @Test
        fun `SCANでキーが空の場合はdelが呼ばれない`() {
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(false)

            redisCacheService.invalidatePattern("openpos:product:*")

            verify(keyCommands, never()).del(any<String>())
        }

        @Test
        fun `Redis例外時はスローせずログ出力する`() {
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection refused"))
            redisCacheService.invalidatePattern("openpos:product:*")
        }
    }
}
