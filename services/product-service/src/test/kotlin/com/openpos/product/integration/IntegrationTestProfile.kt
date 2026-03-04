package com.openpos.product.integration

import io.quarkus.test.junit.QuarkusTestProfile
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

/**
 * Testcontainers で PostgreSQL を起動する結合テストプロファイル。
 * Flyway マイグレーションを有効化し、実際の DB スキーマでテストする。
 */
class IntegrationTestProfile : QuarkusTestProfile {
    companion object {
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("openpos_test")
                .withUsername("test")
                .withPassword("test")
                .apply {
                    start()
                    createSchemas()
                }

        private fun PostgreSQLContainer<*>.createSchemas() {
            DriverManager.getConnection(jdbcUrl, username, password).use { conn ->
                conn.createStatement().use { stmt ->
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS product_schema")
                }
            }
        }
    }

    override fun getConfigOverrides(): Map<String, String> =
        mapOf(
            "quarkus.datasource.db-kind" to "postgresql",
            "quarkus.datasource.jdbc.url" to postgres.jdbcUrl,
            "quarkus.datasource.username" to postgres.username,
            "quarkus.datasource.password" to postgres.password,
            "quarkus.hibernate-orm.database.generation" to "none",
            "quarkus.hibernate-orm.database.default-schema" to "product_schema",
            "quarkus.flyway.migrate-at-start" to "true",
            "quarkus.flyway.schemas" to "product_schema",
            "quarkus.datasource.health.enabled" to "false",
            "quarkus.redis.devservices.enabled" to "false",
            "quarkus.rabbitmq.devservices.enabled" to "false",
            "quarkus.grpc.server.port" to "0",
            "quarkus.grpc.server.test-port" to "0",
            "quarkus.http.port" to "0",
            "quarkus.http.test-port" to "0",
        )

    override fun getConfigProfile(): String = "integration"
}
