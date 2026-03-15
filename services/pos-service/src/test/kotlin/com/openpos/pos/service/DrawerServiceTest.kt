package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.repository.DrawerRepository
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.util.UUID

@QuarkusTest
class DrawerServiceTest {
    @Inject
    lateinit var drawerService: DrawerService

    @Inject
    lateinit var drawerRepository: DrawerRepository

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val terminalId = UUID.randomUUID()

    @BeforeEach
    @Transactional
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        drawerRepository.deleteAll()
    }

    @Nested
    inner class OpenDrawer {
        @Test
        fun `ドロワーを正常に開局する`() {
            // Act
            val drawer = drawerService.openDrawer(storeId, terminalId, 30000L)

            // Assert
            assertNotNull(drawer.id)
            assertEquals(orgId, drawer.organizationId)
            assertEquals(storeId, drawer.storeId)
            assertEquals(terminalId, drawer.terminalId)
            assertEquals(30000L, drawer.openingAmount)
            assertEquals(30000L, drawer.currentAmount)
            assertTrue(drawer.isOpen)
            assertNotNull(drawer.openedAt)
        }

        @Test
        fun `つり銭準備金0円でも開局できる`() {
            // Act
            val drawer = drawerService.openDrawer(storeId, terminalId, 0L)

            // Assert
            assertEquals(0L, drawer.openingAmount)
            assertEquals(0L, drawer.currentAmount)
            assertTrue(drawer.isOpen)
        }

        @Test
        fun `既に開いているドロワーがある場合はエラー`() {
            // Arrange
            drawerService.openDrawer(storeId, terminalId, 30000L)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                drawerService.openDrawer(storeId, terminalId, 30000L)
            }
        }
    }

    @Nested
    inner class CloseDrawer {
        @Test
        fun `ドロワーを正常に閉局する`() {
            // Arrange
            drawerService.openDrawer(storeId, terminalId, 30000L)

            // Act
            val closed = drawerService.closeDrawer(storeId, terminalId)

            // Assert
            assertFalse(closed.isOpen)
            assertNotNull(closed.closedAt)
        }

        @Test
        fun `開いているドロワーがない場合はエラー`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                drawerService.closeDrawer(storeId, terminalId)
            }
        }

        @Test
        fun `閉局後に再度開局できる`() {
            // Arrange
            drawerService.openDrawer(storeId, terminalId, 30000L)
            drawerService.closeDrawer(storeId, terminalId)

            // Act
            val reopened = drawerService.openDrawer(storeId, terminalId, 50000L)

            // Assert
            assertTrue(reopened.isOpen)
            assertEquals(50000L, reopened.openingAmount)
        }
    }

    @Nested
    inner class GetDrawerStatus {
        @Test
        fun `開局中のドロワー状態を取得する`() {
            // Arrange
            drawerService.openDrawer(storeId, terminalId, 30000L)

            // Act
            val status = drawerService.getDrawerStatus(storeId, terminalId)

            // Assert
            assertTrue(status.isOpen)
            assertEquals(30000L, status.openingAmount)
        }

        @Test
        fun `ドロワーが存在しない場合はエラー`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                drawerService.getDrawerStatus(storeId, terminalId)
            }
        }
    }
}
