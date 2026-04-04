package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.ProductAlertEntity
import com.openpos.analytics.repository.ProductAlertRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class ProductAlertServiceTest {
    private lateinit var productAlertService: ProductAlertService
    private lateinit var productAlertRepository: ProductAlertRepository
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        productAlertRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        productAlertService = ProductAlertService()
        productAlertService.productAlertRepository = productAlertRepository
        productAlertService.tenantFilterService = tenantFilterService
        productAlertService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class ListByOrganization {
        @Test
        fun `returns alerts with total count`() {
            // Arrange
            val alerts = listOf(createAlert(), createAlert())
            whenever(productAlertRepository.findByOrganizationId(any(), any<Page>()))
                .thenReturn(alerts)
            whenever(productAlertRepository.countByOrganizationId(orgId))
                .thenReturn(5L)

            // Act
            val (result, count) = productAlertService.listByOrganization(orgId, 0, 20)

            // Assert
            assertEquals(2, result.size)
            assertEquals(5L, count)
        }

        @Test
        fun `returns empty list when no alerts`() {
            // Arrange
            whenever(productAlertRepository.findByOrganizationId(any(), any<Page>()))
                .thenReturn(emptyList())
            whenever(productAlertRepository.countByOrganizationId(orgId))
                .thenReturn(0L)

            // Act
            val (result, count) = productAlertService.listByOrganization(orgId, 0, 20)

            // Assert
            assertTrue(result.isEmpty())
            assertEquals(0L, count)
        }
    }

    @Nested
    inner class ListUnread {
        @Test
        fun `returns only unread alerts`() {
            // Arrange
            val alerts = listOf(createAlert(isRead = false))
            whenever(productAlertRepository.findUnreadByOrganizationId(orgId))
                .thenReturn(alerts)

            // Act
            val result = productAlertService.listUnread(orgId)

            // Assert
            assertEquals(1, result.size)
        }

        @Test
        fun `returns empty when all alerts are read`() {
            // Arrange
            whenever(productAlertRepository.findUnreadByOrganizationId(orgId))
                .thenReturn(emptyList())

            // Act
            val result = productAlertService.listUnread(orgId)

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `creates and returns new alert`() {
            // Arrange
            val productId = UUID.randomUUID()
            doNothing().whenever(productAlertRepository).persist(any<ProductAlertEntity>())

            // Act
            val result = productAlertService.create(orgId, productId, "TRENDING", "Sales up 50%")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(productId, result.productId)
            assertEquals("TRENDING", result.alertType)
            assertEquals("Sales up 50%", result.description)
        }
    }

    @Nested
    inner class MarkAsRead {
        @Test
        fun `marks alert as read when owned by current tenant`() {
            // Arrange
            val alertId = UUID.randomUUID()
            val alert = createAlert(id = alertId, isRead = false, alertOrgId = orgId)
            val mockQuery1 = mock<PanacheQuery<ProductAlertEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(alert)
            whenever(productAlertRepository.find(eq("id = ?1"), eq(alertId))).thenReturn(mockQuery1)
            doNothing().whenever(productAlertRepository).persist(any<ProductAlertEntity>())

            // Act
            val result = productAlertService.markAsRead(alertId)

            // Assert
            assertNotNull(result)
            assertTrue(result!!.isRead)
        }

        @Test
        fun `returns null when alert not found`() {
            // Arrange
            val alertId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<ProductAlertEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(productAlertRepository.find(eq("id = ?1"), eq(alertId))).thenReturn(mockQuery2)

            // Act
            val result = productAlertService.markAsRead(alertId)

            // Assert
            assertNull(result)
        }

        @Test
        fun `returns null when alert belongs to different tenant`() {
            // Arrange
            val alertId = UUID.randomUUID()
            val otherOrgId = UUID.randomUUID()
            val alert = createAlert(id = alertId, isRead = false, alertOrgId = otherOrgId)
            val mockQuery3 = mock<PanacheQuery<ProductAlertEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(alert)
            whenever(productAlertRepository.find(eq("id = ?1"), eq(alertId))).thenReturn(mockQuery3)

            // Act
            val result = productAlertService.markAsRead(alertId)

            // Assert
            assertNull(result)
        }
    }

    // === Helpers ===

    private fun createAlert(
        id: UUID = UUID.randomUUID(),
        isRead: Boolean = false,
        alertOrgId: UUID = orgId,
    ): ProductAlertEntity =
        ProductAlertEntity().apply {
            this.id = id
            this.organizationId = alertOrgId
            this.productId = UUID.randomUUID()
            this.alertType = "TRENDING"
            this.description = "Test alert"
            this.isRead = isRead
        }
}
