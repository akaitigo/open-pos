package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.NotificationEntity
import com.openpos.store.repository.NotificationRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class NotificationServiceTest {
    @Inject
    lateinit var notificationService: NotificationService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var notificationRepository: NotificationRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `通知を正常に作成する`() {
            // Arrange
            doNothing().whenever(notificationRepository).persist(any<NotificationEntity>())

            // Act
            val result = notificationService.create("ALERT", "在庫不足", "商品Aの在庫が残り5個です")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals("ALERT", result.type)
            assertEquals("在庫不足", result.title)
            assertEquals("商品Aの在庫が残り5個です", result.message)
            assertFalse(result.isRead)
            verify(notificationRepository).persist(any<NotificationEntity>())
        }

        @Test
        fun `organizationIdが未設定の場合はエラー`() {
            // Arrange
            organizationIdHolder.organizationId = null

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                notificationService.create("ALERT", "テスト", "テストメッセージ")
            }
        }

        @Test
        fun `INFOタイプの通知を作成する`() {
            // Arrange
            doNothing().whenever(notificationRepository).persist(any<NotificationEntity>())

            // Act
            val result = notificationService.create("INFO", "システム更新", "新機能が追加されました")

            // Assert
            assertEquals("INFO", result.type)
            assertEquals("システム更新", result.title)
            assertEquals("新機能が追加されました", result.message)
        }
    }

    // === list ===

    @Nested
    inner class List {
        @Test
        fun `通知一覧をページネーションで取得する`() {
            // Arrange
            val notification1 = createNotificationEntity("ALERT", "通知1", "メッセージ1")
            val notification2 = createNotificationEntity("INFO", "通知2", "メッセージ2")
            whenever(notificationRepository.listPaginated(any()))
                .thenReturn(listOf(notification1, notification2))
            whenever(notificationRepository.count()).thenReturn(2L)

            // Act
            val (items, total) = notificationService.list(0, 20)

            // Assert
            assertEquals(2, items.size)
            assertEquals(2L, total)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `通知が存在しない場合は空リストと0を返す`() {
            // Arrange
            whenever(notificationRepository.listPaginated(any())).thenReturn(emptyList())
            whenever(notificationRepository.count()).thenReturn(0L)

            // Act
            val (items, total) = notificationService.list(0, 20)

            // Assert
            assertEquals(0, items.size)
            assertEquals(0L, total)
        }
    }

    // === countUnread ===

    @Nested
    inner class CountUnread {
        @Test
        fun `未読通知数を取得する`() {
            // Arrange
            whenever(notificationRepository.countUnread()).thenReturn(5L)

            // Act
            val result = notificationService.countUnread()

            // Assert
            assertEquals(5L, result)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `未読通知が0件の場合は0を返す`() {
            // Arrange
            whenever(notificationRepository.countUnread()).thenReturn(0L)

            // Act
            val result = notificationService.countUnread()

            // Assert
            assertEquals(0L, result)
        }
    }

    // === markAsRead ===

    @Nested
    inner class MarkAsRead {
        @Test
        fun `通知を既読にする`() {
            // Arrange
            val notificationId = UUID.randomUUID()
            val entity = createNotificationEntity("ALERT", "テスト", "テスト本文")
            entity.id = notificationId
            whenever(notificationRepository.findById(notificationId)).thenReturn(entity)
            doNothing().whenever(notificationRepository).persist(any<NotificationEntity>())

            // Act
            val result = notificationService.markAsRead(notificationId)

            // Assert
            assertTrue(result)
            assertTrue(entity.isRead)
            verify(tenantFilterService).enableFilter()
            verify(notificationRepository).persist(any<NotificationEntity>())
        }

        @Test
        fun `存在しない通知IDの場合はfalseを返す`() {
            // Arrange
            val notificationId = UUID.randomUUID()
            whenever(notificationRepository.findById(notificationId)).thenReturn(null)

            // Act
            val result = notificationService.markAsRead(notificationId)

            // Assert
            assertFalse(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === helper ===

    private fun createNotificationEntity(
        type: String,
        title: String,
        message: String,
    ): NotificationEntity =
        NotificationEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.type = type
            this.title = title
            this.message = message
            this.isRead = false
        }
}
