package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.webhook.WebhookDeliveryService
import com.openpos.gateway.webhook.WebhookRegistration
import com.openpos.gateway.webhook.WebhookStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class WebhookResourceTest {
    private val webhookStore: WebhookStore = mock()
    private val deliveryService: WebhookDeliveryService = mock()
    private val tenantContext = TenantContext().apply { staffRole = "OWNER" }
    private val resource =
        WebhookResource().also { r ->
            ProductResourceTest.setField(r, "webhookStore", webhookStore)
            ProductResourceTest.setField(r, "deliveryService", deliveryService)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        tenantContext.organizationId = orgId
        tenantContext.staffRole = "OWNER"
    }

    @Nested
    inner class UpdateRBAC {
        @Test
        fun `CASHIERはWebhook更新できない`() {
            // Arrange
            tenantContext.staffRole = "CASHIER"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.update(UUID.randomUUID().toString(), UpdateWebhookBody(url = "https://example.com"))
            }
        }

        @Test
        fun `MANAGERはWebhook更新できる`() {
            // Arrange
            tenantContext.staffRole = "MANAGER"
            val webhookId = UUID.randomUUID()
            val existing =
                WebhookRegistration(
                    id = webhookId,
                    organizationId = orgId,
                    url = "https://example.com/hook",
                    events = listOf("sale.completed"),
                    secret = "a".repeat(32),
                )
            whenever(webhookStore.findById(webhookId)).thenReturn(existing)
            whenever(webhookStore.update(any())).thenReturn(existing)

            // Act
            val response = resource.update(webhookId.toString(), UpdateWebhookBody(events = listOf("sale.completed", "product.created")))

            // Assert
            assertEquals(200, response.status)
        }
    }

    @Nested
    inner class DeleteRBAC {
        @Test
        fun `CASHIERはWebhook削除できない`() {
            // Arrange
            tenantContext.staffRole = "CASHIER"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.delete(UUID.randomUUID().toString())
            }
        }

        @Test
        fun `OWNERはWebhook削除できる`() {
            // Arrange
            val webhookId = UUID.randomUUID()
            val existing =
                WebhookRegistration(
                    id = webhookId,
                    organizationId = orgId,
                    url = "https://example.com/hook",
                    events = listOf("sale.completed"),
                    secret = "a".repeat(32),
                )
            whenever(webhookStore.findById(webhookId)).thenReturn(existing)
            whenever(webhookStore.delete(webhookId)).thenReturn(true)

            // Act
            val response = resource.delete(webhookId.toString())

            // Assert
            assertEquals(204, response.status)
        }
    }

    @Nested
    inner class TestDeliveryRBAC {
        @Test
        fun `CASHIERはテスト配信できない`() {
            // Arrange
            tenantContext.staffRole = "CASHIER"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.testDelivery(TestWebhookBody(eventType = "sale.completed", payload = "{}"))
            }
        }

        @Test
        fun `MANAGERはテスト配信できる`() {
            // Arrange
            tenantContext.staffRole = "MANAGER"
            whenever(deliveryService.deliver(any(), any(), any())).thenReturn(emptyList())

            // Act
            val response = resource.testDelivery(TestWebhookBody(eventType = "sale.completed", payload = "{}"))

            // Assert
            assertEquals(200, response.status)
        }
    }

    @Nested
    inner class RetryPendingRBAC {
        @Test
        fun `CASHIERはリトライ実行できない`() {
            // Arrange
            tenantContext.staffRole = "CASHIER"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.retryPending()
            }
        }

        @Test
        fun `OWNERはリトライ実行できる`() {
            // Arrange
            whenever(deliveryService.retryPendingDeliveries(orgId)).thenReturn(2)

            // Act
            val response = resource.retryPending()

            // Assert
            assertEquals(200, response.status)
        }
    }
}
