package com.openpos.store.cache

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

class StoreCacheServiceTest {
    private val redisDataSource: RedisDataSource = mock()
    private val valueCommands: ValueCommands<String, String> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val cacheService =
        StoreCacheService().also {
            val field = StoreCacheService::class.java.getDeclaredField("redis")
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
            val key = "openpos:store-service:store:123"
            whenever(valueCommands.get(key)).thenReturn("cached-store")
            val result = cacheService.get(key)
            assertEquals("cached-store", result)
        }

        @Test
        fun `キャッシュミス時はnullを返す`() {
            val key = "openpos:store-service:store:999"
            whenever(valueCommands.get(key)).thenReturn(null)
            val result = cacheService.get(key)
            assertNull(result)
        }

        @Test
        fun `Redis例外時はnullを返す`() {
            val key = "openpos:store-service:store:123"
            whenever(valueCommands.get(key)).thenThrow(RuntimeException("Connection lost"))
            val result = cacheService.get(key)
            assertNull(result)
        }
    }

    @Nested
    inner class Set {
        @Test
        fun `デフォルトTTL（600秒）でsetexが呼ばれる`() {
            cacheService.set("openpos:store-service:store:123", "value")
            verify(valueCommands).setex("openpos:store-service:store:123", 600L, "value")
        }

        @Test
        fun `カスタムTTLでsetexが呼ばれる`() {
            cacheService.set("openpos:store-service:store:123", "value", ttlSeconds = 120L)
            verify(valueCommands).setex("openpos:store-service:store:123", 120L, "value")
        }

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            whenever(valueCommands.setex(any(), any(), any())).thenThrow(RuntimeException("Connection lost"))
            cacheService.set("openpos:store-service:store:123", "value")
        }
    }

    @Nested
    inner class Invalidate {
        @Test
        fun `単一キーでdelが呼ばれる`() {
            cacheService.invalidate("openpos:store-service:store:123")
            verify(keyCommands).del("openpos:store-service:store:123")
        }

        @Test
        fun `複数キーでdelが全キーで呼ばれる`() {
            cacheService.invalidate("openpos:store-service:store:1", "openpos:store-service:store:2")
            verify(keyCommands).del("openpos:store-service:store:1", "openpos:store-service:store:2")
        }

        @Test
        fun `空のキーではdelが呼ばれない`() {
            cacheService.invalidate()
            verify(keyCommands, never()).del(any<String>())
        }

        @Test
        fun `Redis例外時は例外をスローせずログ出力する`() {
            whenever(keyCommands.del(any<String>())).thenThrow(RuntimeException("Connection lost"))
            cacheService.invalidate("openpos:store-service:store:123")
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

            cacheService.invalidatePattern("openpos:store-service:store:*")

            verify(keyCommands).del("key1", "key2")
        }

        @Test
        fun `SCANでキーが空の場合はdelが呼ばれない`() {
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(false)

            cacheService.invalidatePattern("openpos:store-service:store:*")

            verify(keyCommands, never()).del(any<String>())
        }

        @Test
        fun `Redis例外時はスローせずログ出力する`() {
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection lost"))
            cacheService.invalidatePattern("openpos:store-service:store:*")
        }
    }

    @Nested
    inner class GetOrLoad {
        @Test
        fun `キャッシュヒット時はloaderを呼ばずにキャッシュ値を返す`() {
            val key = "openpos:store-service:store:123"
            whenever(valueCommands.get(key)).thenReturn("cached-value")

            val result = cacheService.getOrLoad(key) { "loaded-value" }

            assertEquals("cached-value", result)
            verify(valueCommands, never()).setnx(any(), any())
        }

        @Test
        fun `キャッシュミスでロック取得成功時はloaderを呼んでキャッシュに設定する`() {
            val key = "openpos:store-service:store:456"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)

            val result = cacheService.getOrLoad(key) { "loaded-value" }

            assertEquals("loaded-value", result)
            verify(valueCommands).setex(key, 600L, "loaded-value")
            verify(keyCommands).del(lockKey)
        }

        @Test
        fun `loaderがnullを返す場合はキャッシュに設定せずnullを返す`() {
            val key = "openpos:store-service:store:789"
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
            val key = "openpos:store-service:store:retry"
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
            val key = "openpos:store-service:store:err"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(any(), any())).thenThrow(RuntimeException("Redis down"))

            val result = cacheService.getOrLoad(key) { "fallback-value" }

            assertEquals("fallback-value", result)
        }

        @Test
        fun `ロック解放失敗時でもloaderの結果は返される`() {
            val key = "openpos:store-service:store:lock-del-fail"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(true)
            whenever(keyCommands.del(lockKey)).thenThrow(RuntimeException("Redis down"))

            val result = cacheService.getOrLoad(key) { "loaded-despite-error" }

            assertEquals("loaded-despite-error", result)
        }

        @Test
        fun `ロック取得失敗でリトライ全て失敗した場合はloaderを直接呼ぶ`() {
            val key = "openpos:store-service:store:timeout"
            val lockKey = "$key:lock"
            whenever(valueCommands.get(key)).thenReturn(null)
            whenever(valueCommands.setnx(lockKey, "1")).thenReturn(false)

            val result = cacheService.getOrLoad(key) { "direct-load" }

            assertEquals("direct-load", result)
        }

        @Test
        fun `カスタムTTLでgetOrLoadが動作する`() {
            val key = "openpos:store-service:store:custom-ttl"
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
        fun `organizationKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals("openpos:store-service:org-abc:org:org-123", cacheService.organizationKey("org-abc", "org-123"))
        }

        @Test
        fun `storeKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals("openpos:store-service:org-abc:store:store-123", cacheService.storeKey("org-abc", "store-123"))
        }

        @Test
        fun `terminalListKeyはorgIdを含む正しい形式のキーを生成する`() {
            assertEquals("openpos:store-service:org-abc:terminal:list:store-456", cacheService.terminalListKey("org-abc", "store-456"))
        }
    }

    @Nested
    inner class InvalidationHelpers {
        @Test
        fun `invalidateOrganizationは組織キーを削除する`() {
            cacheService.invalidateOrganization("org-abc", "org-123")
            verify(keyCommands).del("openpos:store-service:org-abc:org:org-123")
        }

        @Test
        fun `invalidateStoreは店舗キーを削除する`() {
            cacheService.invalidateStore("org-abc", "store-123")
            verify(keyCommands).del("openpos:store-service:org-abc:store:store-123")
        }

        @Test
        fun `invalidateTerminalListは端末リストキーを削除する`() {
            cacheService.invalidateTerminalList("org-abc", "store-456")
            verify(keyCommands).del("openpos:store-service:org-abc:terminal:list:store-456")
        }
    }
}
