package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.ProductAlertEntity
import com.openpos.analytics.repository.ProductAlertRepository
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
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class ProductAlertServiceUnitTest {
    private lateinit var service: ProductAlertService
    private lateinit var productAlertRepository: ProductAlertRepository
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        productAlertRepository = mock()
        organizationIdHolder = OrganizationIdHolder()
        tenantFilterService = mock()

        service = ProductAlertService()
        service.productAlertRepository = productAlertRepository
        service.organizationIdHolder = organizationIdHolder
        service.tenantFilterService = tenantFilterService

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(productAlertRepository).persist(any<ProductAlertEntity>())
    }

    @Nested
    inner class ListByOrganization {
        @Test
        fun `returns alerts with total count`() {
            val alerts = listOf(createAlert(), createAlert())
            whenever(productAlertRepository.findByOrganizationId(any(), any<Page>())).thenReturn(alerts)
            whenever(productAlertRepository.countByOrganizationId(orgId)).thenReturn(5L)

            val (result, count) = service.listByOrganization(orgId, 0, 20)

            assertEquals(2, result.size)
            assertEquals(5L, count)
        }
    }

    @Nested
    inner class ListUnread {
        @Test
        fun `returns unread alerts`() {
            val alerts = listOf(createAlert(isRead = false))
            whenever(productAlertRepository.findUnreadByOrganizationId(orgId)).thenReturn(alerts)

            val result = service.listUnread(orgId)

            assertEquals(1, result.size)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `creates alert`() {
            val productId = UUID.randomUUID()

            val result = service.create(orgId, productId, "TRENDING", "Sales up 50%")

            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("TRENDING", result.alertType)
            verify(productAlertRepository).persist(any<ProductAlertEntity>())
        }
    }

    @Nested
    inner class MarkAsRead {
        @Test
        fun `marks alert as read when owned by current tenant`() {
            val alertId = UUID.randomUUID()
            val alert = createAlert(id = alertId, alertOrgId = orgId)
            whenever(productAlertRepository.findById(alertId)).thenReturn(alert)

            val result = service.markAsRead(alertId)

            assertNotNull(result)
            assertTrue(result!!.isRead)
        }

        @Test
        fun `returns null when alert not found`() {
            val alertId = UUID.randomUUID()
            whenever(productAlertRepository.findById(alertId)).thenReturn(null)

            assertNull(service.markAsRead(alertId))
        }

        @Test
        fun `returns null when alert belongs to different tenant`() {
            val alertId = UUID.randomUUID()
            val otherOrgId = UUID.randomUUID()
            val alert = createAlert(id = alertId, alertOrgId = otherOrgId)
            whenever(productAlertRepository.findById(alertId)).thenReturn(alert)

            assertNull(service.markAsRead(alertId))
        }
    }

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
