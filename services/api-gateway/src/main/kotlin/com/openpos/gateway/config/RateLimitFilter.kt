package com.openpos.gateway.config

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
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * テナント（organization_id）単位のレートリミッター。
 * 1分あたりの最大リクエスト数を制限する。
 * organization_id が未設定のリクエストは "anonymous" としてカウント。
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 5)
@ApplicationScoped
class RateLimitFilter :
    ContainerRequestFilter,
    ContainerResponseFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    @ConfigProperty(name = "openpos.rate-limit.requests-per-minute", defaultValue = "1000")
    var requestsPerMinute: Int = 1000

    companion object {
        private const val WINDOW_MILLIS = 60_000L
        private const val ANONYMOUS_KEY = "anonymous"
    }

    private val buckets = ConcurrentHashMap<String, RateBucket>()

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
        val bucket = buckets.computeIfAbsent(tenantKey) { RateBucket() }
        val remaining = bucket.tryConsume(requestsPerMinute)

        // レスポンス用にリクエストプロパティに保存
        requestContext.setProperty("rateLimit.limit", requestsPerMinute)
        requestContext.setProperty("rateLimit.remaining", remaining.coerceAtLeast(0))
        requestContext.setProperty("rateLimit.resetAt", bucket.windowResetEpochSecond())

        if (remaining < 0) {
            requestContext.abortWith(
                Response
                    .status(429)
                    .header("X-RateLimit-Limit", requestsPerMinute)
                    .header("X-RateLimit-Remaining", 0)
                    .header("X-RateLimit-Reset", bucket.windowResetEpochSecond())
                    .header("Retry-After", bucket.retryAfterSeconds())
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
     * 固定ウィンドウ方式のレートバケット。
     * スレッドセーフに1分間のリクエスト数をカウントする。
     */
    internal class RateBucket {
        private val windowStart = AtomicLong(System.currentTimeMillis())
        private val count = AtomicInteger(0)

        /**
         * リクエストを1つ消費し、残りリクエスト数を返す。
         * 負の値はレートリミット超過を意味する。
         */
        fun tryConsume(limit: Int): Int {
            val now = System.currentTimeMillis()
            val start = windowStart.get()

            // ウィンドウのリセット
            if (now - start >= WINDOW_MILLIS) {
                if (windowStart.compareAndSet(start, now)) {
                    count.set(0)
                }
            }

            val current = count.incrementAndGet()
            return limit - current
        }

        fun windowResetEpochSecond(): Long = Instant.ofEpochMilli(windowStart.get() + WINDOW_MILLIS).epochSecond

        fun retryAfterSeconds(): Long {
            val remainingMillis = (windowStart.get() + WINDOW_MILLIS) - System.currentTimeMillis()
            return (remainingMillis / 1000).coerceAtLeast(1)
        }
    }
}
