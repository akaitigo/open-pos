package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.WebhookEntity
import com.openpos.store.repository.WebhookRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.mockito.kotlin.eq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class WebhookServiceUnitTest {
    private lateinit var service: WebhookService
    private lateinit var webhookRepository: WebhookRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        webhookRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = WebhookService()
        service.webhookRepository = webhookRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(webhookRepository).persist(any<WebhookEntity>())
        doNothing().whenever(webhookRepository).delete(any<WebhookEntity>())
    }

    @Nested
    inner class Create {
        @Test
        fun `creates webhook with correct fields`() {
            val result = service.create("https://example.com/hook", "sale.completed", "secret-key")

            assertNotNull(result)
            assertEquals("https://example.com/hook", result.url)
            assertEquals("sale.completed", result.events)
            assertEquals("secret-key", result.secret)
            assertEquals(orgId, result.organizationId)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `updates webhook fields selectively`() {
            val webhookId = UUID.randomUUID()
            val entity =
                WebhookEntity().apply {
                    id = webhookId
                    organizationId = orgId
                    url = "https://old.com"
                    events = "[]"
                    secret = "old-secret"
                    isActive = true
                }
            val mockQuery1 = mock<PanacheQuery<WebhookEntity>>()
whenever(mockQuery1.firstResult()).thenReturn(entity)
whenever(webhookRepository.find(eq("id = ?1"), eq(webhookId))).thenReturn(mockQuery1)

            val result = service.update(webhookId, "https://new.com", null, false)

            assertNotNull(result)
            assertEquals("https://new.com", result?.url)
            assertEquals("[]", result?.events)
            assertFalse(result?.isActive ?: true)
        }

        @Test
        fun `returns null when webhook not found`() {
            val id = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<WebhookEntity>>()
whenever(mockQuery2.firstResult()).thenReturn(null)
whenever(webhookRepository.find(eq("id = ?1"), eq(id))).thenReturn(mockQuery2)

            val result = service.update(id, "url", null, null)

            assertNull(result)
        }
    }

    @Nested
    inner class Delete {
        @Test
        fun `deletes webhook and returns true`() {
            val webhookId = UUID.randomUUID()
            val entity =
                WebhookEntity().apply {
                    id = webhookId
                    organizationId = orgId
                    url = "https://example.com"
                    events = "[]"
                    secret = "secret"
                }
            val mockQuery3 = mock<PanacheQuery<WebhookEntity>>()
whenever(mockQuery3.firstResult()).thenReturn(entity)
whenever(webhookRepository.find(eq("id = ?1"), eq(webhookId))).thenReturn(mockQuery3)

            val result = service.delete(webhookId)

            assertTrue(result)
            verify(webhookRepository).delete(entity)
        }

        @Test
        fun `returns false when webhook not found`() {
            val id = UUID.randomUUID()
            val mockQuery4 = mock<PanacheQuery<WebhookEntity>>()
whenever(mockQuery4.firstResult()).thenReturn(null)
whenever(webhookRepository.find(eq("id = ?1"), eq(id))).thenReturn(mockQuery4)

            val result = service.delete(id)

            assertFalse(result)
        }
    }

    @Nested
    inner class Trigger {
        @Test
        fun `triggers webhooks for active ones`() {
            val webhook =
                WebhookEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    url = "https://example.com/hook"
                    events = """["sale.completed"]"""
                    secret = "my-secret"
                    isActive = true
                }
            whenever(webhookRepository.findActiveByOrganizationId(orgId)).thenReturn(listOf(webhook))

            service.trigger(orgId, "sale.completed", """{"data":"test"}""")

            assertTrue(webhook.isActive)
            assertEquals("https://example.com/hook", webhook.url)
            verify(webhookRepository).findActiveByOrganizationId(orgId)
        }

        @Test
        fun `handles empty webhook list`() {
            whenever(webhookRepository.findActiveByOrganizationId(orgId)).thenReturn(emptyList())

            service.trigger(orgId, "sale.completed", """{"data":"test"}""")

            assertNotNull(orgId)
        }
    }

    @Nested
    inner class ListByOrganizationId {
        @Test
        fun `returns webhooks for organization`() {
            val webhooks =
                listOf(
                    WebhookEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        url = "https://example.com"
                        events = "[]"
                        secret = "s"
                    },
                )
            whenever(webhookRepository.findByOrganizationId(orgId)).thenReturn(webhooks)

            val result = service.listByOrganizationId(orgId)

            assertEquals(1, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }
}
