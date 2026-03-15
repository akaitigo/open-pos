package com.openpos.inventory

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Liveness
import org.eclipse.microprofile.health.Readiness

@Liveness
@ApplicationScoped
class ServiceLivenessCheck : HealthCheck {
    override fun call(): HealthCheckResponse = HealthCheckResponse.up("inventory-service")
}

/**
 * Readiness check: DB + Redis の接続を検証する。
 */
@Readiness
@ApplicationScoped
class ServiceReadinessCheck : HealthCheck {
    @Inject
    lateinit var entityManager: EntityManager

    @Inject
    lateinit var redis: ReactiveRedisDataSource

    override fun call(): HealthCheckResponse {
        val builder = HealthCheckResponse.named("inventory-service-readiness")
        try {
            entityManager.createNativeQuery("SELECT 1").singleResult
            builder.withData("database", "ok")
        } catch (e: Exception) {
            builder.withData("database", "error: ${e.message}")
            return builder.down().build()
        }
        try {
            redis
                .execute()
                .pong()
                .await()
                .indefinitely()
            builder.withData("redis", "ok")
        } catch (e: Exception) {
            builder.withData("redis", "error: ${e.message}")
            return builder.down().build()
        }
        return builder.up().build()
    }
}
