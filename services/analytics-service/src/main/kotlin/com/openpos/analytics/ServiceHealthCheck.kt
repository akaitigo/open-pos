package com.openpos.analytics

import io.quarkus.redis.datasource.ReactiveRedisDataSource
import io.smallrye.reactive.messaging.providers.extension.HealthCenter
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
    override fun call(): HealthCheckResponse = HealthCheckResponse.up("analytics-service")
}

/**
 * Readiness check: DB + Redis + RabbitMQ の接続を検証する。
 */
@Readiness
@ApplicationScoped
class ServiceReadinessCheck : HealthCheck {
    @Inject
    lateinit var entityManager: EntityManager

    @Inject
    lateinit var redis: ReactiveRedisDataSource

    @Inject
    lateinit var healthCenter: HealthCenter

    override fun call(): HealthCheckResponse {
        val builder = HealthCheckResponse.named("analytics-service-readiness")
        try {
            entityManager.createNativeQuery("SELECT 1").singleResult
            builder.withData("database", "ok")
        } catch (e: Exception) {
            builder.withData("database", "error: ${e.message}")
            return builder.down().build()
        }
        try {
            redis
                .execute("PING")
                .await()
                .indefinitely()
            builder.withData("redis", "ok")
        } catch (e: Exception) {
            builder.withData("redis", "error: ${e.message}")
            return builder.down().build()
        }
        if (!healthCenter.readiness.isOk) {
            builder.withData("rabbitmq", "not ready")
            return builder.down().build()
        }
        builder.withData("rabbitmq", "ok")
        return builder.up().build()
    }
}
