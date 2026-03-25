package com.openpos.inventory.config

import io.micrometer.core.instrument.config.MeterFilter
import jakarta.inject.Singleton

@Singleton
class MicrometerConfig {
    @jakarta.enterprise.inject.Produces
    @Singleton
    fun denyRedisPoolMetrics(): MeterFilter = MeterFilter.deny { it.name.startsWith("redis.pool.") }
}
