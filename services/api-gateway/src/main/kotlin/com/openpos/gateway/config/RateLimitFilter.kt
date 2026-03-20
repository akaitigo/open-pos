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
 * Redis INCR + EXPIRE による固定ウィンドウ方式で、1分あたりの最大リクエスト数を制限する。
 * organization_id が未設定のリクエストは "anonymous" としてカウント。
 * 複数インスタンスでも正しく動作する分散レートリミッター。
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
        private const val WINDOW_SECONDS = 60L
        private const val ANONYMOUS_KEY = "anonymous"
        private const val KEY_PREFIX = "openpos:rate-limit:"
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
        val windowKey = currentWindowKey(tenantKey)

        val (current, resetAt) =
            try {
                incrementAndGetCount(windowKey)
            } catch (e: Exception) {
                log.warnf("Redis rate-limit failed, allowing request: %s", e.message)
                // Redis 障害時はリクエストを許可する（フォールバック）
                return
            }

        val remaining = (requestsPerMinute - current).coerceAtLeast(0)

        // レスポンス用にリクエストプロパティに保存
        requestContext.setProperty("rateLimit.limit", requestsPerMinute)
        requestContext.setProperty("rateLimit.remaining", remaining)
        requestContext.setProperty("rateLimit.resetAt", resetAt)

        if (current > requestsPerMinute) {
            val retryAfter = (resetAt - Instant.now().epochSecond).coerceAtLeast(1)
            requestContext.abortWith(
                Response
                    .status(429)
                    .header("X-RateLimit-Limit", requestsPerMinute)
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", resetAt)
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
     * Redis INCR + EXPIRE で現在ウィンドウのカウントをアトミックにインクリメントする。
     * @return Pair<現在のカウント, ウィンドウリセット時刻(epoch秒)>
     */
    private fun incrementAndGetCount(key: String): Pair<Int, Long> {
        val commands = redis.value(Long::class.java)
        val count = commands.incr(key)

        // 初回（count == 1）のときだけ TTL を設定
        if (count == 1L) {
            redis.key().expire(key, WINDOW_SECONDS)
        }

        val ttl = redis.key().ttl(key)
        val resetAt = Instant.now().epochSecond + ttl.coerceAtLeast(0)

        return Pair(count.toInt(), resetAt)
    }

    /**
     * 現在のウィンドウに対応する Redis キーを生成する。
     * 固定ウィンドウ: epoch 秒を 60 秒で割った値をウィンドウ ID とする。
     */
    private fun currentWindowKey(tenantKey: String): String {
        val windowId = Instant.now().epochSecond / WINDOW_SECONDS
        return "$KEY_PREFIX$tenantKey:$windowId"
    }
}
