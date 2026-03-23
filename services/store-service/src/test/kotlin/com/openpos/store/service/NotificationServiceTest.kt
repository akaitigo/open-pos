package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.NotificationEntity
import com.openpos.store.repository.NotificationRepository
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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

class NotificationServiceTest {
    private lateinit var service: NotificationService
    private lateinit var notificationRepository: NotificationRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        notificationRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = NotificationService()
        service.notificationRepository = notificationRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class Create {
        @Test
        fun `creates notification with correct fields`() {
            doNothing().whenever(notificationRepository).persist(any<NotificationEntity>())

            val result = service.create("ALERT", "Low Stock", "Product X is low")

            assertEquals("ALERT", result.type)
            assertEquals("Low Stock", result.title)
            assertEquals("Product X is low", result.message)
            assertEquals(orgId, result.organizationId)
            verify(notificationRepository).persist(any<NotificationEntity>())
        }
    }

    @Nested
    inner class ListTest {
        @Test
        fun `returns paginated notifications`() {
            val items =
                listOf(
                    NotificationEntity().apply {
                        this.id = UUID.randomUUID()
                        this.organizationId = orgId
                        this.type = "ALERT"
                        this.title = "Alert 1"
                        this.message = "msg1"
                    },
                )
            whenever(notificationRepository.listPaginated(any<Page>())).thenReturn(items)
            whenever(notificationRepository.count()).thenReturn(1L)

            val (result, total) = service.list(0, 20)

            assertEquals(1, result.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class CountUnread {
        @Test
        fun `returns unread count`() {
            whenever(notificationRepository.countUnread()).thenReturn(5L)

            val result = service.countUnread()

            assertEquals(5L, result)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class MarkAsRead {
        @Test
        fun `marks notification as read and returns true`() {
            val notifId = UUID.randomUUID()
            val entity =
                NotificationEntity().apply {
                    this.id = notifId
                    this.organizationId = orgId
                    this.type = "ALERT"
                    this.title = "Alert"
                    this.message = "msg"
                    this.isRead = false
                }
            whenever(notificationRepository.findById(notifId)).thenReturn(entity)
            doNothing().whenever(notificationRepository).persist(any<NotificationEntity>())

            val result = service.markAsRead(notifId)

            assertTrue(result)
            assertTrue(entity.isRead)
            verify(notificationRepository).persist(any<NotificationEntity>())
        }

        @Test
        fun `returns false when notification not found`() {
            val notifId = UUID.randomUUID()
            whenever(notificationRepository.findById(notifId)).thenReturn(null)

            val result = service.markAsRead(notifId)

            assertFalse(result)
        }
    }
}
