package com.openpos.gateway.config

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.annotation.Priority
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import java.time.Instant

/**
 * PIN 認証エンドポイント専用の per-IP レートリミッター。
 * ブルートフォース攻撃を防止するため、1分あたり 10 回に制限する。
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 3)
@ApplicationScoped
class PinAuthRateLimitFilter : ContainerRequestFilter {
    @Inject
    lateinit var redis: RedisDataSource

    @ConfigProperty(name = "openpos.rate-limit.pin-auth-per-minute", defaultValue = "10")
    var pinAuthPerMinute: Int = 10

    private val log = Logger.getLogger(PinAuthRateLimitFilter::class.java)

    companion object {
        private const val TTL_SECONDS = 60L
        private val PIN_AUTH_PATH_REGEX = Regex("^api/staff/[^/]+/authenticate$")
    }

    override fun filter(requestContext: ContainerRequestContext) {
        if (!requestContext.method.equals("POST", ignoreCase = true)) return

        val path = requestContext.uriInfo.path
        if (!PIN_AUTH_PATH_REGEX.matches(path)) return

        val clientIp = extractClientIp(requestContext)
        val minuteKey = (Instant.now().epochSecond / 60).toString()
        val redisKey = "openpos:api-gateway:pin-ratelimit:$clientIp:$minuteKey"

        val currentCount = tryIncrement(redisKey)

        if (currentCount < 0 || currentCount > pinAuthPerMinute) {
            log.warnf("PIN auth rate limit exceeded: ip=%s, count=%d", clientIp, currentCount)
            requestContext.abortWith(
                Response
                    .status(429)
                    .header("Retry-After", 60)
                    .entity(
                        mapOf(
                            "error" to "Too Many Requests",
                            "message" to "Too many PIN authentication attempts. Please try again later.",
                        ),
                    ).build(),
            )
        }
    }

    private fun extractClientIp(requestContext: ContainerRequestContext): String =
        requestContext
            .getHeaderString("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?: requestContext.getHeaderString("X-Real-IP")
            ?: "unknown"

    internal fun tryIncrement(key: String): Long =
        try {
            val valueCommands = redis.value(Long::class.java)
            val count = valueCommands.incr(key)
            if (count == 1L) {
                redis.key().expire(key, TTL_SECONDS)
            }
            count
        } catch (e: Exception) {
            log.warnf("Redis PIN rate-limit INCR failed for key=%s: %s", key, e.message)
            -1L
        }
}
