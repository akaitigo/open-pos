package com.openpos.gateway.webhook

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class WebhookDeliveryServiceTest {
    private lateinit var webhookStore: WebhookStore
    private lateinit var deliveryService: WebhookDeliveryService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        webhookStore = WebhookStore()
        deliveryService = WebhookDeliveryService()
        deliveryService.webhookStore = webhookStore
    }

    @Nested
    inner class CalculateBackoff {
        @Test
        fun `attempt 1 returns base backoff`() {
            // Arrange & Act
            val result = WebhookDeliveryService.calculateBackoff(1)

            // Assert
            assertEquals(30L, result)
        }

        @Test
        fun `attempt 2 returns double backoff`() {
            // Arrange & Act
            val result = WebhookDeliveryService.calculateBackoff(2)

            // Assert
            assertEquals(60L, result)
        }

        @Test
        fun `attempt 3 returns quadruple backoff`() {
            // Arrange & Act
            val result = WebhookDeliveryService.calculateBackoff(3)

            // Assert
            assertEquals(120L, result)
        }

        @Test
        fun `attempt 5 returns 16x backoff`() {
            // Arrange & Act
            val result = WebhookDeliveryService.calculateBackoff(5)

            // Assert
            assertEquals(480L, result)
        }

        @Test
        fun `large attempt is capped`() {
            // Arrange & Act
            val result = WebhookDeliveryService.calculateBackoff(20)

            // Assert
            assertTrue(result > 0)
            assertTrue(result <= 30L * (1L shl 10))
        }
    }

    @Nested
    inner class ComputeSignature {
        @Test
        fun `computes HMAC-SHA256 signature with sha256 prefix`() {
            // Arrange
            val payload = """{"event":"sale.completed","data":{}}"""
            val secret = "test-secret-key"

            // Act
            val signature = WebhookDeliveryService.computeSignature(payload, secret)

            // Assert
            assertTrue(signature.startsWith("sha256="))
            assertEquals(71, signature.length) // "sha256=" (7) + 64 hex chars
        }

        @Test
        fun `same payload and secret produce same signature`() {
            // Arrange
            val payload = """{"test":true}"""
            val secret = "my-secret"

            // Act
            val sig1 = WebhookDeliveryService.computeSignature(payload, secret)
            val sig2 = WebhookDeliveryService.computeSignature(payload, secret)

            // Assert
            assertEquals(sig1, sig2)
        }

        @Test
        fun `different secrets produce different signatures`() {
            // Arrange
            val payload = """{"test":true}"""

            // Act
            val sig1 = WebhookDeliveryService.computeSignature(payload, "secret-1")
            val sig2 = WebhookDeliveryService.computeSignature(payload, "secret-2")

            // Assert
            assertTrue(sig1 != sig2)
        }
    }

    @Nested
    inner class WebhookStoreOperations {
        @Test
        fun `registers and retrieves a webhook`() {
            // Arrange
            val registration =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "test-secret",
                )

            // Act
            webhookStore.register(registration)
            val found = webhookStore.findById(registration.id)

            // Assert
            assertNotNull(found)
            assertEquals("https://example.com/webhook", found?.url)
            assertEquals(listOf("sale.completed"), found?.events)
        }

        @Test
        fun `finds webhooks by organization`() {
            // Arrange
            val otherId = UUID.randomUUID()
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/a",
                    events = listOf("sale.completed"),
                    secret = "secret",
                ),
            )
            webhookStore.register(
                WebhookRegistration(
                    organizationId = otherId,
                    url = "https://example.com/b",
                    events = listOf("sale.completed"),
                    secret = "secret",
                ),
            )

            // Act
            val results = webhookStore.findByOrganization(orgId)

            // Assert
            assertEquals(1, results.size)
            assertEquals("https://example.com/a", results[0].url)
        }

        @Test
        fun `finds active webhooks by event type`() {
            // Arrange
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/sales",
                    events = listOf("sale.completed", "sale.voided"),
                    secret = "secret",
                    isActive = true,
                ),
            )
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/products",
                    events = listOf("product.created"),
                    secret = "secret",
                    isActive = true,
                ),
            )
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/inactive",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = false,
                ),
            )

            // Act
            val results = webhookStore.findActiveByEvent(orgId, "sale.completed")

            // Assert
            assertEquals(1, results.size)
            assertEquals("https://example.com/sales", results[0].url)
        }

        @Test
        fun `deletes a webhook`() {
            // Arrange
            val registration =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            webhookStore.register(registration)

            // Act
            val deleted = webhookStore.delete(registration.id)

            // Assert
            assertTrue(deleted)
            assertEquals(null, webhookStore.findById(registration.id))
        }

        @Test
        fun `delete returns false for non-existent webhook`() {
            // Act
            val deleted = webhookStore.delete(UUID.randomUUID())

            // Assert
            assertEquals(false, deleted)
        }
    }

    @Nested
    inner class DeliveryTracking {
        @Test
        fun `records and retrieves delivery`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            val delivery =
                WebhookDelivery(
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )

            // Act
            webhookStore.recordDelivery(delivery)
            val deliveries = webhookStore.findDeliveries(webhookId)

            // Assert
            assertEquals(1, deliveries.size)
            assertEquals(DeliveryStatus.PENDING, deliveries[0].status)
        }

        @Test
        fun `updates delivery status`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            val delivery =
                WebhookDelivery(
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )
            webhookStore.recordDelivery(delivery)

            // Act
            webhookStore.updateDelivery(
                delivery.copy(
                    status = DeliveryStatus.SUCCESS,
                    httpStatusCode = 200,
                    attemptCount = 1,
                    completedAt = Instant.now(),
                ),
            )
            val deliveries = webhookStore.findDeliveries(webhookId)

            // Assert
            assertEquals(1, deliveries.size)
            assertEquals(DeliveryStatus.SUCCESS, deliveries[0].status)
            assertEquals(200, deliveries[0].httpStatusCode)
            assertEquals(1, deliveries[0].attemptCount)
        }

        @Test
        fun `finds pending retries only when nextRetryAt is past`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            val pastRetry =
                WebhookDelivery(
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            val futureRetry =
                WebhookDelivery(
                    webhookId = webhookId,
                    eventType = "sale.completed",
                    payload = "{}",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    nextRetryAt = Instant.now().plusSeconds(3600),
                )
            webhookStore.recordDelivery(pastRetry)
            webhookStore.recordDelivery(futureRetry)

            // Act
            val pending = webhookStore.findPendingRetries()

            // Assert
            assertEquals(1, pending.size)
            assertEquals(pastRetry.id, pending[0].id)
        }
    }

    @Nested
    inner class HandleFailureLogic {
        @Test
        fun `marks delivery as RETRYING when under max retries`() {
            // Arrange
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            webhookStore.register(webhook)

            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                    attemptCount = 0,
                    maxRetries = 5,
                )
            webhookStore.recordDelivery(delivery)

            // Act — deliver to invalid URL will fail
            val result = deliveryService.attemptDelivery(webhook, delivery)

            // Assert
            assertEquals(DeliveryStatus.RETRYING, result.status)
            assertEquals(1, result.attemptCount)
            assertNotNull(result.nextRetryAt)
            assertNotNull(result.lastError)
        }

        @Test
        fun `marks delivery as FAILED when max retries exceeded`() {
            // Arrange
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            webhookStore.register(webhook)

            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 4,
                    maxRetries = 5,
                )
            webhookStore.recordDelivery(delivery)

            // Act — 5th attempt (attemptCount=4 + 1 = 5 = maxRetries)
            val result = deliveryService.attemptDelivery(webhook, delivery)

            // Assert
            assertEquals(DeliveryStatus.FAILED, result.status)
            assertEquals(5, result.attemptCount)
            assertNotNull(result.completedAt)
        }
    }

    @Nested
    inner class DeliverToAllWebhooks {
        @Test
        fun `returns empty list when no webhooks registered`() {
            // Act
            val results = deliveryService.deliver(orgId, "sale.completed", "{}")

            // Assert
            assertEquals(0, results.size)
        }

        @Test
        fun `delivers to matching webhooks only`() {
            // Arrange
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/sales",
                    events = listOf("sale.completed"),
                    secret = "secret",
                ),
            )
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/products",
                    events = listOf("product.created"),
                    secret = "secret",
                ),
            )

            // Act
            val results = deliveryService.deliver(orgId, "sale.completed", """{"event":"sale.completed"}""")

            // Assert
            assertEquals(1, results.size)
        }
    }
}
