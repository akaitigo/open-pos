package com.openpos.product.integration

import io.quarkus.test.junit.QuarkusTestProfile

/**
 * H2 ベースの結合テストプロファイル。
 * application-integration.properties を使用し、Testcontainers を必要としない軽量プロファイル。
 * Redis / RabbitMQ も無効化し、DB アクセスのみをテスト対象とする。
 */
class H2IntegrationTestProfile : QuarkusTestProfile {
    override fun getConfigProfile(): String = "integration"
}
