package com.openpos.gateway.resource

import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.value.ValueCommands
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured.given
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

/**
 * Quarkus REST (rest-jackson) の JSON ボディ・デシリアライズ回帰テスト。
 *
 * Quarkus 3.37.1 で「全プロパティにデフォルト値を持つ Kotlin data class」の
 * リクエストボディが JSON の値を無視してデフォルト値でバインドされる回帰があり、
 * E2E (docker-smoke) の POST /api/transactions/{id}/items が
 * "Invalid UUID: "（空 productId）で失敗した。HTTP 層を通さない unit test では
 * 検知できないため、本テストで実際のデシリアライズ経路を検証する。
 */
@Path("/__test/deser")
class DeserializationProbeResource {
    @POST
    @Path("/add-item")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun addItem(body: AddItemBody): Map<String, Any?> =
        mapOf(
            "productId" to body.productId,
            "quantity" to body.quantity,
            "customProductName" to body.customProductName,
        )

    @POST
    @Path("/create-transaction")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun createTransaction(body: CreateTransactionBody): Map<String, Any?> = mapOf("storeId" to body.storeId, "type" to body.type)
}

@QuarkusTest
class KotlinBodyDeserializationTest {
    @InjectMock
    lateinit var redis: RedisDataSource

    @BeforeEach
    fun bypassRateLimit() {
        // RateLimitFilter は Redis 障害時 fail-closed (429) のため、正常応答をモックする
        val valueCommands = mock<ValueCommands<String, Long>>()
        whenever(valueCommands.incr(any())).thenReturn(1L)
        whenever(redis.value(Long::class.java)).thenReturn(valueCommands)
        val keyCommands = mock<KeyCommands<String>>()
        whenever(redis.key()).thenReturn(keyCommands)
    }

    @Test
    fun `AddItemBody - 全デフォルト値付きdata classでもJSONの値がバインドされる`() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"productId":"11111111-1111-1111-1111-111111111111","quantity":2}""")
            .`when`()
            .post("/__test/deser/add-item")
            .then()
            .statusCode(200)
            .body("productId", `is`("11111111-1111-1111-1111-111111111111"))
            .body("quantity", `is`(2))
    }

    @Test
    fun `AddItemBody - 省略フィールドはデフォルト値になる`() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"productId":"22222222-2222-2222-2222-222222222222"}""")
            .`when`()
            .post("/__test/deser/add-item")
            .then()
            .statusCode(200)
            .body("productId", `is`("22222222-2222-2222-2222-222222222222"))
            .body("quantity", `is`(1))
    }

    @Test
    fun `CreateTransactionBody - デフォルト値なしdata classのバインド`() {
        given()
            .contentType(MediaType.APPLICATION_JSON)
            .body("""{"storeId":"s-1","terminalId":"t-1","staffId":"u-1","type":"SALE"}""")
            .`when`()
            .post("/__test/deser/create-transaction")
            .then()
            .statusCode(200)
            .body("storeId", `is`("s-1"))
            .body("type", `is`("SALE"))
    }
}
