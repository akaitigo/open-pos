package com.openpos.gateway.config

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.container.ContainerResponseContext
import jakarta.ws.rs.container.ContainerResponseFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Instant

/**
 * テナント（organization_id）単位のレートリミッター。
 * 1分あたりの最大リクエスト数を制限する。
 * organization_id が未設定のリクエストは "anonymous" としてカウント。
 * Redis INCR + EXPIRE による分散レートリミット。
 * Redis 障害時はリクエストを許可する（fail-open）。
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 5)
@ApplicationScoped
class RateLimitFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    @Inject
    lateinit var redis: RedisDataSource

    @ConfigProperty(name = "openpos.rate-limit.requests-per-minute", defaultValue = "1000")
    var requestsPerMinute: Int = 1000

    private val log = Logger.getLogger(RateLimitFilter::class.java)

    companion object {
        private const val TTL_SECONDS = 60L
        private const val ANONYMOUS_KEY = "anonymous"
    }

    override fun filter(requestContext: ContainerRequestContext) {
        // OPTIONS は常にスキップ
        if (requestContext.method.equals("OPTIONS", ignoreCase = true)) {
            return
        }

        // Health エンドポイントはスキップ
        val path = requestContext.uriInfo.path
        if (path.startsWith("api/health") || path.startsWith("q/")) {
            return
        }

        val tenantKey = tenantContext.organizationId?.toString() ?: ANONYMOUS_KEY
        val minuteKey = currentMinuteKey()
        val redisKey = "openpos:api-gateway:ratelimit:$tenantKey:$minuteKey"

        val currentCount = tryIncrement(redisKey)

        // Redis 障害時は -1 が返る → fail-open でリクエスト許可
        if (currentCount < 0) {
            return
        }

        val remaining = requestsPerMinute - currentCount.toInt()
        val resetEpochSecond = nextMinuteEpochSecond()

        // レスポンス用にリクエストプロパティに保存
        requestContext.setProperty("rateLimit.limit", requestsPerMinute)
        requestContext.setProperty("rateLimit.remaining", remaining.coerceAtLeast(0))
        requestContext.setProperty("rateLimit.resetAt", resetEpochSecond)

        if (remaining < 0) {
            val retryAfter = (resetEpochSecond - Instant.now().epochSecond).coerceAtLeast(1)
            requestContext.abortWith(
                Response
                    .status(429)
                    .header("X-RateLimit-Limit", requestsPerMinute)
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", resetEpochSecond)
                    .header("Retry-After", retryAfter)
                    .entity(mapOf("error" to "Too Many Requests", "message" to "Rate limit exceeded. Try again later."))
                    .build(),
            )
        }
    }

    override fun filter(
        requestContext: ContainerRequestContext,
        responseContext: ContainerResponseContext,
    ) {
        val limit = requestContext.getProperty("rateLimit.limit") as? Int ?: return
        val remaining = requestContext.getProperty("rateLimit.remaining") as? Int ?: return
        val resetAt = requestContext.getProperty("rateLimit.resetAt") as? Long ?: return

        responseContext.headers.putSingle("X-RateLimit-Limit", limit)
        responseContext.headers.putSingle("X-RateLimit-Remaining", remaining)
        responseContext.headers.putSingle("X-RateLimit-Reset", resetAt)
    }

    /**
     * Redis INCR + EXPIRE でリクエスト数をカウントする。
     * Redis 障害時は -1 を返す（fail-open）。
     */
    internal fun tryIncrement(key: String): Long =
        try {
            val valueCommands = redis.value(Long::class.java)
            val count = valueCommands.incr(key)
            if (count == 1L) {
                redis.key().expire(key, TTL_SECONDS)
            }
            count
        } catch (e: Exception) {
            log.warnf("Redis rate-limit INCR failed for key=%s: %s", key, e.message)
            -1L
        }

    /**
     * 現在の分を表すキー文字列（エポック分）。
     */
    internal fun currentMinuteKey(): String {
        val now = Instant.now()
        val epochMinute = now.epochSecond / 60
        return epochMinute.toString()
    }

    /**
     * 次の分の開始エポック秒を返す。
     */
    internal fun nextMinuteEpochSecond(): Long {
        val now = Instant.now()
        val currentMinute = now.epochSecond / 60
        return (currentMinute + 1) * 60
    }
}
