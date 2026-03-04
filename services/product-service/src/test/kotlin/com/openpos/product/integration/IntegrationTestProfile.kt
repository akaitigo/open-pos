package com.openpos.product.integration

import io.quarkus.test.junit.QuarkusTestProfile
import org.testcontainers.containers.PostgreSQLContainer

/**
 * Testcontainers で PostgreSQL を起動する結合テストプロファイル。
 * Flyway の create-schemas=true でスキーマを自動作成し、マイグレーションで実テーブルを構築する。
 */
class IntegrationTestProfile : QuarkusTestProfile {
    companion object {
        val postgres: PostgreSQLContainer<*> =
            PostgreSQLContainer("postgres:17-alpine")
                .withDatabaseName("openpos_test")
                .withUsername("test")
                .withPassword("test")
                .apply { start() }
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
            "quarkus.flyway.create-schemas" to "true",
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
