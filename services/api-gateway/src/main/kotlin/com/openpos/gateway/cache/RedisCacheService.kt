package com.openpos.gateway.cache

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class RedisCacheService {
    @Inject
    lateinit var redis: RedisDataSource

    companion object {
        private const val DEFAULT_TTL_SECONDS = 3600L
    }

    fun get(key: String): String? = redis.string(String::class.java).get(key)

    fun set(
        key: String,
        value: String,
        ttlSeconds: Long = DEFAULT_TTL_SECONDS,
    ) {
        redis.string(String::class.java).setex(key, ttlSeconds, value)
    }

    fun invalidate(vararg keys: String) {
        if (keys.isNotEmpty()) {
            redis.key().del(*keys)
        }
    }

    fun invalidatePattern(pattern: String) {
        val keys = redis.key().keys(pattern)
        if (keys.isNotEmpty()) {
            redis.key().del(*keys.toTypedArray())
        }
    }
}
