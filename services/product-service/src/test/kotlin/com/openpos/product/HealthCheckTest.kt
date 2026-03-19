package com.openpos.product

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import org.hamcrest.Matchers.anyOf
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthCheckTest {
    @Test
    fun `application starts and health endpoint responds`() {
        given()
            .`when`()
            .get("/q/health")
            .then()
            .statusCode(anyOf(`is`(200), `is`(503)))
    }
}
