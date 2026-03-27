package com.openpos.gateway.webhook

import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.net.InetSocketAddress
import java.time.Instant
import java.util.UUID

class WebhookDeliveryServiceTest {
    private val webhookStore: WebhookStore = mock()
    private lateinit var deliveryService: WebhookDeliveryService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        deliveryService = WebhookDeliveryService()
        deliveryService.webhookStore = webhookStore
    }

    @Nested
    inner class CalculateBackoff {
        @Test
        fun `attempt 1 returns base backoff`() {
            val result = WebhookDeliveryService.calculateBackoff(1)
            assertEquals(30L, result)
        }

        @Test
        fun `attempt 2 returns double backoff`() {
            val result = WebhookDeliveryService.calculateBackoff(2)
            assertEquals(60L, result)
        }

        @Test
        fun `attempt 3 returns quadruple backoff`() {
            val result = WebhookDeliveryService.calculateBackoff(3)
            assertEquals(120L, result)
        }

        @Test
        fun `attempt 5 returns 16x backoff`() {
            val result = WebhookDeliveryService.calculateBackoff(5)
            assertEquals(480L, result)
        }

        @Test
        fun `large attempt is capped`() {
            val result = WebhookDeliveryService.calculateBackoff(20)
            assertTrue(result > 0)
            assertTrue(result <= 30L * (1L shl 10))
        }
    }

    @Nested
    inner class ComputeSignature {
        @Test
        fun `computes HMAC-SHA256 signature with sha256 prefix`() {
            val payload = """{"event":"sale.completed","data":{}}"""
            val secret = "test-secret-key"
            val signature = WebhookDeliveryService.computeSignature(payload, secret)
            assertTrue(signature.startsWith("sha256="))
            assertEquals(71, signature.length)
        }

        @Test
        fun `same payload and secret produce same signature`() {
            val payload = """{"test":true}"""
            val secret = "my-secret"
            val sig1 = WebhookDeliveryService.computeSignature(payload, secret)
            val sig2 = WebhookDeliveryService.computeSignature(payload, secret)
            assertEquals(sig1, sig2)
        }

        @Test
        fun `different secrets produce different signatures`() {
            val payload = """{"test":true}"""
            val sig1 = WebhookDeliveryService.computeSignature(payload, "secret-1")
            val sig2 = WebhookDeliveryService.computeSignature(payload, "secret-2")
            assertTrue(sig1 != sig2)
        }
    }

    @Nested
    inner class AttemptDeliveryHttpSuccess {
        @Test
        fun `marks delivery as SUCCESS when HTTP 200`() {
            val server = HttpServer.create(InetSocketAddress(0), 0)
            val port = server.address.port
            server.createContext("/webhook") { exchange ->
                exchange.sendResponseHeaders(200, 0)
                exchange.responseBody.close()
            }
            server.start()

            try {
                val webhook =
                    WebhookRegistration(
                        organizationId = orgId,
                        url = "http://localhost:$port/webhook",
                        events = listOf("sale.completed"),
                        secret = "test-secret",
                    )
                val delivery =
                    WebhookDelivery(
                        webhookId = webhook.id,
                        eventType = "sale.completed",
                        payload = """{"test":true}""",
                        status = DeliveryStatus.PENDING,
                    )
                whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

                val result = deliveryService.attemptDelivery(webhook, delivery)

                assertEquals(DeliveryStatus.SUCCESS, result.status)
                assertEquals(200, result.httpStatusCode)
                assertEquals(1, result.attemptCount)
                assertNotNull(result.completedAt)
            } finally {
                server.stop(0)
            }
        }

        @Test
        fun `marks delivery as RETRYING when HTTP 500`() {
            val server = HttpServer.create(InetSocketAddress(0), 0)
            val port = server.address.port
            server.createContext("/webhook") { exchange ->
                exchange.sendResponseHeaders(500, 0)
                exchange.responseBody.close()
            }
            server.start()

            try {
                val webhook =
                    WebhookRegistration(
                        organizationId = orgId,
                        url = "http://localhost:$port/webhook",
                        events = listOf("sale.completed"),
                        secret = "test-secret",
                    )
                val delivery =
                    WebhookDelivery(
                        webhookId = webhook.id,
                        eventType = "sale.completed",
                        payload = """{"test":true}""",
                        status = DeliveryStatus.PENDING,
                        maxRetries = 5,
                    )
                whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

                val result = deliveryService.attemptDelivery(webhook, delivery)

                assertEquals(DeliveryStatus.RETRYING, result.status)
                assertEquals(500, result.httpStatusCode)
                assertEquals(1, result.attemptCount)
            } finally {
                server.stop(0)
            }
        }
    }

    @Nested
    inner class HandleFailureLogic {
        @Test
        fun `marks delivery as RETRYING when under max retries`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                    attemptCount = 0,
                    maxRetries = 5,
                )
            whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

            val result = deliveryService.attemptDelivery(webhook, delivery)

            assertEquals(DeliveryStatus.RETRYING, result.status)
            assertEquals(1, result.attemptCount)
            assertNotNull(result.nextRetryAt)
            assertNotNull(result.lastError)
        }

        @Test
        fun `marks delivery as FAILED when max retries exceeded`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 4,
                    maxRetries = 5,
                )
            whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

            val result = deliveryService.attemptDelivery(webhook, delivery)

            assertEquals(DeliveryStatus.FAILED, result.status)
            assertEquals(5, result.attemptCount)
            assertNotNull(result.completedAt)
        }
    }

    @Nested
    inner class DeliverToAllWebhooks {
        @Test
        fun `returns empty list when no webhooks registered`() {
            whenever(webhookStore.findActiveByEvent(orgId, "sale.completed"))
                .thenReturn(emptyList())

            val results = deliveryService.deliver(orgId, "sale.completed", "{}")

            assertEquals(0, results.size)
        }

        @Test
        fun `delivers to matching webhooks only`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/sales",
                    events = listOf("sale.completed"),
                    secret = "secret",
                )
            whenever(webhookStore.findActiveByEvent(orgId, "sale.completed"))
                .thenReturn(listOf(webhook))
            whenever(webhookStore.recordDelivery(any())).thenAnswer { it.arguments[0] }
            whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

            val results =
                deliveryService.deliver(
                    orgId,
                    "sale.completed",
                    """{"event":"sale.completed"}""",
                )

            assertEquals(1, results.size)
        }
    }

    @Nested
    inner class RetryPendingDeliveries {
        @Test
        fun `retries deliveries with past nextRetryAt`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    maxRetries = 5,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            whenever(webhookStore.findPendingRetries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)
            whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

            val retried = deliveryService.retryPendingDeliveries()

            assertEquals(1, retried)
        }

        @Test
        fun `skips retries for inactive webhooks`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = false,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    maxRetries = 5,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            whenever(webhookStore.findPendingRetries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)
            whenever(webhookStore.updateDelivery(any())).thenAnswer { it.arguments[0] }

            val retried = deliveryService.retryPendingDeliveries()

            assertEquals(0, retried)
            verify(webhookStore).updateDelivery(any())
        }

        @Test
        fun `filters by organizationId when provided`() {
            val otherOrgId = UUID.randomUUID()
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://invalid.example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    maxRetries = 5,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            whenever(webhookStore.findPendingRetries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)

            val retried = deliveryService.retryPendingDeliveries(otherOrgId)

            assertEquals(0, retried)
        }

        @Test
        fun `returns zero when no pending retries`() {
            whenever(webhookStore.findPendingRetries()).thenReturn(emptyList())

            val retried = deliveryService.retryPendingDeliveries()

            assertEquals(0, retried)
        }

        @Test
        fun `skips delivery when webhook not found`() {
            val delivery =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.RETRYING,
                    attemptCount = 1,
                    maxRetries = 5,
                    nextRetryAt = Instant.now().minusSeconds(60),
                )
            whenever(webhookStore.findPendingRetries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(delivery.webhookId)).thenReturn(null)

            val retried = deliveryService.retryPendingDeliveries()

            assertEquals(0, retried)
        }
    }
}
