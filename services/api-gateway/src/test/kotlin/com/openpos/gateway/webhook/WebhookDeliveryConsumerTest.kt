package com.openpos.gateway.webhook

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class WebhookDeliveryConsumerTest {
    private val webhookStore: WebhookStore = mock()
    private val deliveryService: WebhookDeliveryService = mock()
    private val consumer =
        WebhookDeliveryConsumer().also {
            val storeField = WebhookDeliveryConsumer::class.java.getDeclaredField("webhookStore")
            storeField.isAccessible = true
            storeField.set(it, webhookStore)
            val serviceField = WebhookDeliveryConsumer::class.java.getDeclaredField("deliveryService")
            serviceField.isAccessible = true
            serviceField.set(it, deliveryService)
        }

    private val orgId = UUID.randomUUID()

    @Nested
    inner class ProcessPendingDeliveries {
        @Test
        fun `PENDING配信がなければ何もしない`() {
            whenever(webhookStore.findPendingDeliveries()).thenReturn(emptyList())

            consumer.processPendingDeliveries()

            verify(deliveryService, never()).attemptDelivery(any(), any())
        }

        @Test
        fun `PENDING配信をdeliveryServiceで処理する`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )
            whenever(webhookStore.findPendingDeliveries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)

            consumer.processPendingDeliveries()

            verify(deliveryService).attemptDelivery(webhook, delivery)
        }

        @Test
        fun `Webhook登録が見つからない場合FAILEDにする`() {
            val delivery =
                WebhookDelivery(
                    webhookId = UUID.randomUUID(),
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )
            whenever(webhookStore.findPendingDeliveries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(delivery.webhookId)).thenReturn(null)

            consumer.processPendingDeliveries()

            verify(webhookStore).updateDelivery(any())
            verify(deliveryService, never()).attemptDelivery(any(), any())
        }

        @Test
        fun `非アクティブWebhookの場合FAILEDにする`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = false,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )
            whenever(webhookStore.findPendingDeliveries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)

            consumer.processPendingDeliveries()

            verify(webhookStore).updateDelivery(any())
            verify(deliveryService, never()).attemptDelivery(any(), any())
        }

        @Test
        fun `attemptDelivery例外時もクラッシュしない`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val delivery =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"test":true}""",
                    status = DeliveryStatus.PENDING,
                )
            whenever(webhookStore.findPendingDeliveries()).thenReturn(listOf(delivery))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)
            whenever(deliveryService.attemptDelivery(any(), any())).thenThrow(RuntimeException("Network error"))

            assertDoesNotThrow { consumer.processPendingDeliveries() }
        }

        @Test
        fun `複数のPENDING配信を順番に処理する`() {
            val webhook =
                WebhookRegistration(
                    organizationId = orgId,
                    url = "https://example.com/webhook",
                    events = listOf("sale.completed"),
                    secret = "secret",
                    isActive = true,
                )
            val delivery1 =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"id":1}""",
                    status = DeliveryStatus.PENDING,
                )
            val delivery2 =
                WebhookDelivery(
                    webhookId = webhook.id,
                    eventType = "sale.completed",
                    payload = """{"id":2}""",
                    status = DeliveryStatus.PENDING,
                )
            whenever(webhookStore.findPendingDeliveries()).thenReturn(listOf(delivery1, delivery2))
            whenever(webhookStore.findById(webhook.id)).thenReturn(webhook)

            consumer.processPendingDeliveries()

            verify(deliveryService).attemptDelivery(webhook, delivery1)
            verify(deliveryService).attemptDelivery(webhook, delivery2)
        }
    }

    @Nested
    inner class ProcessRetries {
        @Test
        fun `リトライ成功数を返す`() {
            whenever(deliveryService.retryPendingDeliveries()).thenReturn(3)

            assertDoesNotThrow { consumer.processRetries() }

            verify(deliveryService).retryPendingDeliveries()
        }

        @Test
        fun `リトライ対象なしでも正常動作`() {
            whenever(deliveryService.retryPendingDeliveries()).thenReturn(0)

            assertDoesNotThrow { consumer.processRetries() }
        }

        @Test
        fun `リトライ処理例外時もクラッシュしない`() {
            whenever(deliveryService.retryPendingDeliveries()).thenThrow(RuntimeException("Redis error"))

            assertDoesNotThrow { consumer.processRetries() }
        }
    }
}
