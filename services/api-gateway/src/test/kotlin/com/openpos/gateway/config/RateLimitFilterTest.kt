package com.openpos.gateway.config

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.value.ValueCommands
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class RateLimitFilterTest {
    private val tenantContext = mock<TenantContext>()
    private val redis = mock<RedisDataSource>()
    private val valueCommands = mock<ValueCommands<String, Long>>()
    private val keyCommands = mock<KeyCommands<String>>()

    private lateinit var filter: RateLimitFilter

    @BeforeEach
    fun setUp() {
        whenever(redis.value(Long::class.java)).thenReturn(valueCommands)
        whenever(redis.key()).thenReturn(keyCommands)

        filter =
            RateLimitFilter().also { f ->
                val tcField = RateLimitFilter::class.java.getDeclaredField("tenantContext")
                tcField.isAccessible = true
                tcField.set(f, tenantContext)

                val redisField = RateLimitFilter::class.java.getDeclaredField("redis")
                redisField.isAccessible = true
                redisField.set(f, redis)

                f.requestsPerMinute = 10
            }
    }

    private fun mockRequestContext(
        method: String = "GET",
        path: String = "api/stores",
    ): ContainerRequestContext {
        val ctx = mock<ContainerRequestContext>()
        val uriInfo = mock<UriInfo>()
        whenever(uriInfo.path).thenReturn(path)
        whenever(ctx.uriInfo).thenReturn(uriInfo)
        whenever(ctx.method).thenReturn(method)
        return ctx
    }

    @Nested
    inner class RequestFiltering {
        @Test
        fun `OPTIONSリクエストはレートリミットをスキップする`() {
            // Arrange
            val ctx = mockRequestContext(method = "OPTIONS")

            // Act
            filter.filter(ctx)

            // Assert
            verify(redis, never()).value(any<Class<Long>>())
        }

        @Test
        fun `ヘルスエンドポイントはレートリミットをスキップする`() {
            // Arrange
            val ctx = mockRequestContext(path = "api/health")

            // Act
            filter.filter(ctx)

            // Assert
            verify(redis, never()).value(any<Class<Long>>())
        }

        @Test
        fun `Quarkus内部パスはレートリミットをスキップする`() {
            // Arrange
            val ctx = mockRequestContext(path = "q/health")

            // Act
            filter.filter(ctx)

            // Assert
            verify(redis, never()).value(any<Class<Long>>())
        }

        @Test
        fun `リミット内のリクエストは通過しabortしない`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(java.util.UUID.randomUUID())
            whenever(valueCommands.incr(any())).thenReturn(1L)
            whenever(keyCommands.ttl(any())).thenReturn(60L)

            // Act
            filter.filter(ctx)

            // Assert
            verify(ctx, never()).abortWith(any())
            verify(ctx).setProperty(eq("rateLimit.limit"), eq(10))
            verify(ctx).setProperty(eq("rateLimit.remaining"), eq(9))
        }

        @Test
        fun `リミット超過のリクエストは429で拒否される`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(java.util.UUID.randomUUID())
            whenever(valueCommands.incr(any())).thenReturn(11L)
            whenever(keyCommands.ttl(any())).thenReturn(30L)

            // Act
            filter.filter(ctx)

            // Assert
            val captor = argumentCaptor<Response>()
            verify(ctx).abortWith(captor.capture())
            assertEquals(429, captor.firstValue.status)
        }

        @Test
        fun `初回リクエスト時にRedisキーのTTLが設定される`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(java.util.UUID.randomUUID())
            whenever(valueCommands.incr(any())).thenReturn(1L)
            whenever(keyCommands.ttl(any())).thenReturn(60L)

            // Act
            filter.filter(ctx)

            // Assert
            verify(keyCommands).expire(any(), eq(60L))
        }

        @Test
        fun `2回目以降のリクエストではTTLが再設定されない`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(java.util.UUID.randomUUID())
            whenever(valueCommands.incr(any())).thenReturn(5L)
            whenever(keyCommands.ttl(any())).thenReturn(45L)

            // Act
            filter.filter(ctx)

            // Assert
            verify(keyCommands, never()).expire(any<String>(), any<Long>())
        }

        @Test
        fun `organizationIdがnullの場合はanonymousキーでカウントされる`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(null)
            whenever(valueCommands.incr(any())).thenReturn(1L)
            whenever(keyCommands.ttl(any())).thenReturn(60L)

            // Act
            filter.filter(ctx)

            // Assert
            val keyCaptor = argumentCaptor<String>()
            verify(valueCommands).incr(keyCaptor.capture())
            assert(keyCaptor.firstValue.contains("anonymous"))
        }

        @Test
        fun `Redis障害時はリクエストを許可する`() {
            // Arrange
            val ctx = mockRequestContext()
            whenever(tenantContext.organizationId).thenReturn(java.util.UUID.randomUUID())
            whenever(valueCommands.incr(any())).thenThrow(RuntimeException("Redis connection failed"))

            // Act
            filter.filter(ctx)

            // Assert
            verify(ctx, never()).abortWith(any())
        }
    }
}
