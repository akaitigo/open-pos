package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.WebhookEntity
import com.openpos.store.repository.WebhookRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * WebhookService の純粋ユニットテスト。
 * update / delete は HQL クエリ (find("id = ?1", id)) を使うため
 * PanacheQuery をモックして検証する。
 */
class WebhookServiceTest {
    private lateinit var webhookService: WebhookService
    private lateinit var webhookRepository: WebhookRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        webhookRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = mock()

        webhookService = WebhookService()
        webhookService.webhookRepository = webhookRepository
        webhookService.tenantFilterService = tenantFilterService
        webhookService.organizationIdHolder = organizationIdHolder

        doNothing().whenever(webhookRepository).persist(any<WebhookEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        whenever(organizationIdHolder.organizationId).thenReturn(testOrgId)
    }

    @Test
    fun `create should create webhook with correct fields`() {
        // Act
        val webhook =
            webhookService.create(
                url = "https://example.com/webhook",
                events = """["sale.completed","stock.low"]""",
                secret = "test-secret-key",
            )

        // Assert
        assertNotNull(webhook)
        assertEquals("https://example.com/webhook", webhook.url)
        assertEquals("""["sale.completed","stock.low"]""", webhook.events)
        assertTrue(webhook.isActive)
    }

    @Test
    fun `trigger should process active webhooks`() {
        // Arrange
        val webhook =
            WebhookEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                url = "https://example.com/webhook"
                events = """["sale.completed"]"""
                secret = "test-secret"
                isActive = true
            }
        whenever(webhookRepository.findActiveByOrganizationId(testOrgId)).thenReturn(listOf(webhook))

        // 例外が発生しないことを確認（プレースホルダー実装）
        webhookService.trigger(testOrgId, "sale.completed", """{"transactionId":"123"}""")
    }

    @Test
    fun `update should modify webhook fields`() {
        // Arrange
        val webhookId = UUID.randomUUID()
        val entity =
            WebhookEntity().apply {
                id = webhookId
                organizationId = testOrgId
                url = "https://example.com/old"
                events = "[]"
                secret = "secret"
                isActive = true
            }
        val mockQuery = mock<PanacheQuery<WebhookEntity>>()
        whenever(mockQuery.firstResult()).thenReturn(entity)
        whenever(webhookRepository.find(eq("id = ?1"), eq(webhookId))).thenReturn(mockQuery)

        // Act
        val result = webhookService.update(webhookId, url = "https://example.com/new", events = null, isActive = false)

        // Assert
        assertNotNull(result)
        assertEquals("https://example.com/new", result?.url)
        assertEquals(false, result?.isActive)
    }
}
