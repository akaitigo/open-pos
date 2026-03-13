package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StaffEntity
import com.openpos.store.repository.StaffRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@QuarkusTest
class StaffServiceTest {
    @Inject
    lateinit var staffService: StaffService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var staffRepository: StaffRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `スタッフを正常に作成する`() {
            // Arrange
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.create(storeId, "田中太郎", "tanaka@example.com", "CASHIER", "hashed_pin_123")

            // Assert
            assertNotNull(result)
            assertEquals(orgId, result.organizationId)
            assertEquals(storeId, result.storeId)
            assertEquals("田中太郎", result.name)
            assertEquals("tanaka@example.com", result.email)
            assertEquals("CASHIER", result.role)
            assertEquals("hashed_pin_123", result.pinHash)
            assertTrue(result.isActive)
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `存在するIDでスタッフを取得する`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.email = "tanaka@example.com"
                    this.role = "CASHIER"
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)

            // Act
            val result = staffService.findById(staffId)

            // Assert
            assertNotNull(result)
            assertEquals(staffId, result?.id)
            assertEquals("田中太郎", result?.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            whenever(staffRepository.findById(staffId)).thenReturn(null)

            // Act
            val result = staffService.findById(staffId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === listByStoreId ===

    @Nested
    inner class ListByStoreId {
        @Test
        fun `店舗IDでスタッフ一覧をページネーション取得する`() {
            // Arrange
            val staff1 =
                StaffEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                }
            val staff2 =
                StaffEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "佐藤花子"
                    this.role = "MANAGER"
                }
            whenever(staffRepository.findByStoreId(any(), any())).thenReturn(listOf(staff1, staff2))
            whenever(staffRepository.countByStoreId(storeId)).thenReturn(2L)

            // Act
            val (staffList, totalCount) = staffService.listByStoreId(storeId, 0, 10)

            // Assert
            assertEquals(2, staffList.size)
            assertEquals(2L, totalCount)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `スタッフが存在しない場合は空リストを返す`() {
            // Arrange
            whenever(staffRepository.findByStoreId(any(), any())).thenReturn(emptyList())
            whenever(staffRepository.countByStoreId(storeId)).thenReturn(0L)

            // Act
            val (staffList, totalCount) = staffService.listByStoreId(storeId, 0, 10)

            // Assert
            assertTrue(staffList.isEmpty())
            assertEquals(0L, totalCount)
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `スタッフ名のみを更新する`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "旧名前"
                    this.email = "old@example.com"
                    this.role = "CASHIER"
                    this.pinHash = "old_hash"
                    this.isActive = true
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.update(staffId, "新名前", null, null, null, null)

            // Assert
            assertNotNull(result)
            assertEquals("新名前", result?.name)
            assertEquals("old@example.com", result?.email)
            assertEquals("CASHIER", result?.role)
            assertEquals("old_hash", result?.pinHash)
            assertTrue(result?.isActive == true)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `全フィールドを一括更新する`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "旧名前"
                    this.email = "old@example.com"
                    this.role = "CASHIER"
                    this.pinHash = "old_hash"
                    this.isActive = true
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.update(staffId, "新名前", "new@example.com", "MANAGER", "new_hash", false)

            // Assert
            assertNotNull(result)
            assertEquals("新名前", result?.name)
            assertEquals("new@example.com", result?.email)
            assertEquals("MANAGER", result?.role)
            assertEquals("new_hash", result?.pinHash)
            assertFalse(result?.isActive == true)
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            whenever(staffRepository.findById(staffId)).thenReturn(null)

            // Act
            val result = staffService.update(staffId, "新名前", null, null, null, null)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === authenticateByPin ===

    @Nested
    inner class AuthenticateByPin {
        @Test
        fun `正しいPINで認証に成功する`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 0
                    this.pinLockedUntil = null
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertTrue(result.success)
            assertNotNull(result.staff)
            assertEquals(staffId, result.staff?.id)
            assertNull(result.reason)
            assertEquals(0, result.staff?.pinFailedCount)
            assertNull(result.staff?.pinLockedUntil)
        }

        @Test
        fun `存在しないスタッフIDの場合はNOT_FOUNDを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            whenever(staffRepository.findById(staffId)).thenReturn(null)

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertFalse(result.success)
            assertNull(result.staff)
            assertEquals("NOT_FOUND", result.reason)
        }

        @Test
        fun `別店舗のスタッフIDの場合はNOT_FOUNDを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val otherStoreId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = otherStoreId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertFalse(result.success)
            assertNull(result.staff)
            assertEquals("NOT_FOUND", result.reason)
        }

        @Test
        fun `無効化されたアカウントの場合はACCOUNT_INACTIVEを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = false
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertFalse(result.success)
            assertNotNull(result.staff)
            assertEquals("ACCOUNT_INACTIVE", result.reason)
        }

        @Test
        fun `ロック中のアカウントの場合はACCOUNT_LOCKEDを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 5
                    this.pinLockedUntil = Instant.now().plusSeconds(3600)
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertFalse(result.success)
            assertNotNull(result.staff)
            assertEquals("ACCOUNT_LOCKED", result.reason)
        }

        @Test
        fun `PINが未設定の場合はPIN_NOT_SETを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = null
                    this.isActive = true
                    this.pinFailedCount = 0
                    this.pinLockedUntil = null
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertFalse(result.success)
            assertNotNull(result.staff)
            assertEquals("PIN_NOT_SET", result.reason)
        }

        @Test
        fun `不正なPINの場合はINVALID_PINを返す`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 0
                    this.pinLockedUntil = null
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "9999") { _, _ -> false }

            // Assert
            assertFalse(result.success)
            assertNotNull(result.staff)
            assertEquals("INVALID_PIN", result.reason)
            assertEquals(1, result.staff?.pinFailedCount)
        }

        @Test
        fun `PIN失敗が最大回数に達するとアカウントがロックされる`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 4
                    this.pinLockedUntil = null
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "9999") { _, _ -> false }

            // Assert
            assertFalse(result.success)
            assertEquals("INVALID_PIN", result.reason)
            assertEquals(5, result.staff?.pinFailedCount)
            assertNotNull(result.staff?.pinLockedUntil)
        }

        @Test
        fun `認証成功時にpinFailedCountがリセットされる`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 3
                    this.pinLockedUntil = null
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertTrue(result.success)
            assertEquals(0, result.staff?.pinFailedCount)
            assertNull(result.staff?.pinLockedUntil)
        }

        @Test
        fun `ロック期間が過ぎた場合はロック解除されて認証処理が続行する`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 5
                    this.pinLockedUntil = Instant.now().minusSeconds(3600)
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "1234") { plain, hash -> plain == hash }

            // Assert
            assertTrue(result.success)
            assertEquals(0, result.staff?.pinFailedCount)
            assertNull(result.staff?.pinLockedUntil)
        }

        @Test
        fun `ロック期間過ぎた後にPINを間違えると失敗カウントが1からリスタートする`() {
            // Arrange
            val staffId = UUID.randomUUID()
            val entity =
                StaffEntity().apply {
                    this.id = staffId
                    this.organizationId = orgId
                    this.storeId = this@StaffServiceTest.storeId
                    this.name = "田中太郎"
                    this.role = "CASHIER"
                    this.pinHash = "1234"
                    this.isActive = true
                    this.pinFailedCount = 5
                    this.pinLockedUntil = Instant.now().minusSeconds(3600)
                }
            whenever(staffRepository.findById(staffId)).thenReturn(entity)
            doNothing().whenever(staffRepository).persist(any<StaffEntity>())

            // Act
            val result = staffService.authenticateByPin(staffId, storeId, "9999") { _, _ -> false }

            // Assert
            assertFalse(result.success)
            assertEquals("INVALID_PIN", result.reason)
            assertEquals(1, result.staff?.pinFailedCount)
        }
    }
}
