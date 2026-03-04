package com.openpos.analytics

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import io.smallrye.health.SmallRyeHealthReporter
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthCheckTest {
    @Inject
    lateinit var healthReporter: SmallRyeHealthReporter

    @Test
    fun `application starts and health is UP`() {
        val health = healthReporter.health
        assertTrue(health.isDown.not(), "Health status should be UP")
    }
}
