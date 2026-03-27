package com.openpos.gateway.webhook

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.quarkus.redis.datasource.RedisDataSource
import io.quarkus.redis.datasource.keys.KeyCommands
import io.quarkus.redis.datasource.keys.KeyScanCursor
import io.quarkus.redis.datasource.set.SetCommands
import io.quarkus.redis.datasource.value.ValueCommands
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class WebhookStoreTest {
    private val redisDataSource: RedisDataSource = mock()
    private val valueCommands: ValueCommands<String, String> = mock()
    private val setCommands: SetCommands<String, String> = mock()
    private val keyCommands: KeyCommands<String> = mock()

    private val objectMapper: ObjectMapper =
        ObjectMapper()
            .registerKotlinModule()
            .registerModule(JavaTimeModule())

    private val store =
        WebhookStore().also {
            val redisField = WebhookStore::class.java.getDeclaredField("redis")
            redisField.isAccessible = true
            redisField.set(it, redisDataSource)
            val omField = WebhookStore::class.java.getDeclaredField("objectMapper")
            omField.isAccessible = true
            omField.set(it, objectMapper)
        }

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        whenever(redisDataSource.value(String::class.java)).thenReturn(valueCommands)
        whenever(redisDataSource.set(String::class.java)).thenReturn(setCommands)
        whenever(redisDataSource.key()).thenReturn(keyCommands)
    }

    @Nested
    inner class Register {
        @Test
        fun `登録時にRedisにJSON保存と組織インデックス追加が行われる`() {
            // Arrange
            val registration =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "test-secret",
                )

            // Act
            val result = store.register(registration)

            // Assert
            assertEquals(registration.id, result.id)
            verify(valueCommands).set(
                eq("openpos:gateway:webhook:${registration.id}"),
                any(),
            )
            verify(setCommands).sadd(
                eq("openpos:gateway:webhook:org:$orgId"),
                eq(registration.id.toString()),
            )
        }

        @Test
        fun `Redis例外時はそのまま例外をスローする`() {
            // Arrange
            val registration =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "test-secret",
                )
            whenever(valueCommands.set(any(), any())).thenThrow(RuntimeException("Connection refused"))

            // Act & Assert
            org.junit.jupiter.api.assertThrows<RuntimeException> {
                store.register(registration)
            }
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `存在するIDでWebhookを取得できる`() {
            // Arrange
            val id = UUID.randomUUID()
            val registration =
                WebhookRegistration(
                    id = id,
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "test-secret",
                )
            val json = objectMapper.writeValueAsString(registration)
            whenever(valueCommands.get("openpos:gateway:webhook:$id")).thenReturn(json)

            // Act
            val result = store.findById(id)

            // Assert
            assertNotNull(result)
            assertEquals(id, result?.id)
            assertEquals("https://example.com/webhook", result?.url)
        }

        @Test
        fun `存在しないIDではnullを返す`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(valueCommands.get("openpos:gateway:webhook:$id")).thenReturn(null)

            // Act
            val result = store.findById(id)

            // Assert
            assertNull(result)
        }

        @Test
        fun `Redis例外時はnullを返す`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(valueCommands.get(any())).thenThrow(RuntimeException("Connection refused"))

            // Act
            val result = store.findById(id)

            // Assert
            assertNull(result)
        }
    }

    @Nested
    inner class FindByOrganization {
        @Test
        fun `組織IDでWebhook一覧を取得できる`() {
            // Arrange
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val reg1 =
                WebhookRegistration(
                    id = id1,
                    organizationId = orgId,
                    url = "https://example.com/a",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            val reg2 =
                WebhookRegistration(
                    id = id2,
                    organizationId = orgId,
                    url = "https://example.com/b",
                    events = listOf("product.created"),
                    secret = "secret",
                )
            whenever(setCommands.smembers("openpos:gateway:webhook:org:$orgId"))
                .thenReturn(setOf(id1.toString(), id2.toString()))
            whenever(valueCommands.get("openpos:gateway:webhook:$id1"))
                .thenReturn(objectMapper.writeValueAsString(reg1))
            whenever(valueCommands.get("openpos:gateway:webhook:$id2"))
                .thenReturn(objectMapper.writeValueAsString(reg2))

            // Act
            val results = store.findByOrganization(orgId)

            // Assert
            assertEquals(2, results.size)
        }

        @Test
        fun `Redis例外時は空リストを返す`() {
            // Arrange
            whenever(setCommands.smembers(any())).thenThrow(RuntimeException("Connection refused"))

            // Act
            val results = store.findByOrganization(orgId)

            // Assert
            assertEquals(0, results.size)
        }
    }

    @Nested
    inner class FindActiveByEvent {
        @Test
        fun `アクティブかつイベント一致のWebhookのみ返す`() {
            // Arrange
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val id3 = UUID.randomUUID()
            val active =
                WebhookRegistration(
                    id = id1,
                    organizationId = orgId,
                    url = "https://example.com/sales",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val inactive =
                WebhookRegistration(
                    id = id2,
                    organizationId = orgId,
                    url = "https://example.com/inactive",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = false,
                )
            val differentEvent =
                WebhookRegistration(
                    id = id3,
                    organizationId = orgId,
                    url = "https://example.com/products",
                    events = listOf("product.created"),
                    secret = "secret",
                    isActive = true,
                )
            whenever(setCommands.smembers("openpos:gateway:webhook:org:$orgId"))
                .thenReturn(setOf(id1.toString(), id2.toString(), id3.toString()))
            whenever(valueCommands.get("openpos:gateway:webhook:$id1"))
                .thenReturn(objectMapper.writeValueAsString(active))
            whenever(valueCommands.get("openpos:gateway:webhook:$id2"))
                .thenReturn(objectMapper.writeValueAsString(inactive))
            whenever(valueCommands.get("openpos:gateway:webhook:$id3"))
                .thenReturn(objectMapper.writeValueAsString(differentEvent))

            // Act
            val results = store.findActiveByEvent(orgId, "sale.completed")

            // Assert
            assertEquals(1, results.size)
            assertEquals("https://example.com/sales", results[0].url)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `存在するWebhookを削除するとtrueを返す`() {
            // Arrange
            val id = UUID.randomUUID()
            val registration =
                WebhookRegistration(
                    id = id,
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            whenever(valueCommands.get("openpos:gateway:webhook:$id"))
                .thenReturn(objectMapper.writeValueAsString(registration))
            whenever(keyCommands.del(any<String>())).thenReturn(1)

            // Act
            val result = store.delete(id)

            // Assert
            assertTrue(result)
            verify(keyCommands).del("openpos:gateway:webhook:$id")
            verify(setCommands).srem("openpos:gateway:webhook:org:$orgId", id.toString())
        }

        @Test
        fun `存在しないWebhookの削除はfalseを返す`() {
            // Arrange
            val id = UUID.randomUUID()
            whenever(valueCommands.get("openpos:gateway:webhook:$id")).thenReturn(null)

            // Act
            val result = store.delete(id)

            // Assert
            assertFalse(result)
        }
    }

    @Nested
    inner class DeliveryOperations {
        @Test
        fun `配信記録をTTL付きで保存する`() {
            // Arrange
            val delivery =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )

            // Act
            store.recordDelivery(delivery)

            // Assert
            verify(valueCommands).setex(
                eq("openpos:gateway:webhook:delivery:${delivery.id}"),
                eq(604800L), // 7 days
                any(),
            )
            verify(setCommands).sadd(
                eq("openpos:gateway:webhook:delivery:wh:${delivery.webhookId}"),
                eq(delivery.id.toString()),
            )
        }

        @Test
        fun `配信記録の更新で既存TTLを維持する`() {
            // Arrange
            val delivery =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.SUCCESS,
                    attemptCount = 1,
                )
            whenever(keyCommands.ttl("openpos:gateway:webhook:delivery:${delivery.id}"))
                .thenReturn(500000L)

            // Act
            store.updateDelivery(delivery)

            // Assert
            verify(valueCommands).setex(
                eq("openpos:gateway:webhook:delivery:${delivery.id}"),
                eq(500000L),
                any(),
            )
        }

        @Test
        fun `配信記録の更新でTTLが0以下ならデフォルトTTLを使う`() {
            // Arrange
            val delivery =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.SUCCESS,
                )
            whenever(keyCommands.ttl("openpos:gateway:webhook:delivery:${delivery.id}"))
                .thenReturn(-1L)

            // Act
            store.updateDelivery(delivery)

            // Assert
            verify(valueCommands).setex(
                eq("openpos:gateway:webhook:delivery:${delivery.id}"),
                eq(604800L),
                any(),
            )
        }

        @Test
        fun `Webhook IDで配信履歴を取得できる`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            val id1 = UUID.randomUUID()
            val id2 = UUID.randomUUID()
            val d1 =
                WebhookDelivery(
                    id = id1,
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.SUCCESS,
                    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
                )
            val d2 =
                WebhookDelivery(
                    id = id2,
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.PENDING,
                    createdAt = Instant.parse("2026-01-02T00:00:00Z"),
                )
            whenever(setCommands.smembers("openpos:gateway:webhook:delivery:wh:$webhookId"))
                .thenReturn(setOf(id1.toString(), id2.toString()))
            whenever(valueCommands.get("openpos:gateway:webhook:delivery:$id1"))
                .thenReturn(objectMapper.writeValueAsString(d1))
            whenever(valueCommands.get("openpos:gateway:webhook:delivery:$id2"))
                .thenReturn(objectMapper.writeValueAsString(d2))

            // Act
            val results = store.findDeliveries(webhookId)

            // Assert
            assertEquals(2, results.size)
            // createdAt降順でソート
            assertEquals(id2, results[0].id)
            assertEquals(id1, results[1].id)
        }

        @Test
        fun `配信履歴取得時にRedis例外で空リスト返す`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            whenever(setCommands.smembers(any())).thenThrow(RuntimeException("Connection refused"))

            // Act
            val results = store.findDeliveries(webhookId)

            // Assert
            assertEquals(0, results.size)
        }
    }

    @Nested
    inner class FindPendingRetries {
        @Test
        fun `リトライ対象の配信のみ返す`() {
            // Arrange
            val pastRetry =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            val futureRetry =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    nextRetryAt = Instant.now().plusSeconds(3600),
                )
            val success =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.SUCCESS,
                )
            val cursor: KeyScanCursor<String> = mock()
            whenever(keyCommands.scan(any())).thenReturn(cursor)
            whenever(cursor.hasNext()).thenReturn(true, false)
            whenever(cursor.next()).thenReturn(
                setOf(
                    "openpos:gateway:webhook:delivery:${pastRetry.id}",
                    "openpos:gateway:webhook:delivery:${futureRetry.id}",
                    "openpos:gateway:webhook:delivery:${success.id}",
                ),
            )
            whenever(valueCommands.get("openpos:gateway:webhook:delivery:${pastRetry.id}"))
                .thenReturn(objectMapper.writeValueAsString(pastRetry))
            whenever(valueCommands.get("openpos:gateway:webhook:delivery:${futureRetry.id}"))
                .thenReturn(objectMapper.writeValueAsString(futureRetry))
            whenever(valueCommands.get("openpos:gateway:webhook:delivery:${success.id}"))
                .thenReturn(objectMapper.writeValueAsString(success))

            // Act
            val pending = store.findPendingRetries()

            // Assert
            assertEquals(1, pending.size)
            assertEquals(pastRetry.id, pending[0].id)
        }

        @Test
        fun `Redis例外時は空リストを返す`() {
            // Arrange
            whenever(keyCommands.scan(any())).thenThrow(RuntimeException("Connection refused"))

            // Act
            val pending = store.findPendingRetries()

            // Assert
            assertEquals(0, pending.size)
        }
    }
}
