package com.openpos.analytics

import io.quarkus.test.junit.QuarkusTest
import io.smallrye.health.SmallRyeHealthReporter
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthCheckTest {
    @Inject
    lateinit var healthReporter: SmallRyeHealthReporter

    @Test
    fun `application starts and health reporter is available`() {
        assertNotNull(healthReporter, "SmallRyeHealthReporter should be injectable")
    }
}
