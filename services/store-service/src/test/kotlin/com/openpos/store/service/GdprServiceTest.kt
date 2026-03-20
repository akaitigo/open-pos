package com.openpos.store.service

import com.openpos.store.entity.AuditLogEntity
import com.openpos.store.entity.CustomerEntity
import com.openpos.store.entity.DataProcessingConsentEntity
import com.openpos.store.entity.OrganizationEntity
import com.openpos.store.entity.StaffEntity
import com.openpos.store.entity.StoreEntity
import com.openpos.store.repository.AttendanceRepository
import com.openpos.store.repository.AuditLogRepository
import com.openpos.store.repository.CustomerRepository
import com.openpos.store.repository.DataProcessingConsentRepository
import com.openpos.store.repository.FavoriteProductRepository
import com.openpos.store.repository.GiftCardRepository
import com.openpos.store.repository.NotificationRepository
import com.openpos.store.repository.OrganizationRepository
import com.openpos.store.repository.PointTransactionRepository
import com.openpos.store.repository.ShiftRepository
import com.openpos.store.repository.StaffRepository
import com.openpos.store.repository.StoreRepository
import com.openpos.store.repository.SubscriptionRepository
import com.openpos.store.repository.SystemSettingRepository
import com.openpos.store.repository.TerminalRepository
import com.openpos.store.repository.WebhookRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
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

class GdprServiceTest {
    private lateinit var service: GdprService
    private val organizationRepository = mock<OrganizationRepository>()
    private val storeRepository = mock<StoreRepository>()
    private val staffRepository = mock<StaffRepository>()
    private val customerRepository = mock<CustomerRepository>()
    private val terminalRepository = mock<TerminalRepository>()
    private val attendanceRepository = mock<AttendanceRepository>()
    private val shiftRepository = mock<ShiftRepository>()
    private val notificationRepository = mock<NotificationRepository>()
    private val favoriteProductRepository = mock<FavoriteProductRepository>()
    private val pointTransactionRepository = mock<PointTransactionRepository>()
    private val giftCardRepository = mock<GiftCardRepository>()
    private val webhookRepository = mock<WebhookRepository>()
    private val subscriptionRepository = mock<SubscriptionRepository>()
    private val systemSettingRepository = mock<SystemSettingRepository>()
    private val auditLogRepository = mock<AuditLogRepository>()
    private val consentRepository = mock<DataProcessingConsentRepository>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            GdprService().apply {
                this.organizationRepository = this@GdprServiceTest.organizationRepository
                this.storeRepository = this@GdprServiceTest.storeRepository
                this.staffRepository = this@GdprServiceTest.staffRepository
                this.customerRepository = this@GdprServiceTest.customerRepository
                this.terminalRepository = this@GdprServiceTest.terminalRepository
                this.attendanceRepository = this@GdprServiceTest.attendanceRepository
                this.shiftRepository = this@GdprServiceTest.shiftRepository
                this.notificationRepository = this@GdprServiceTest.notificationRepository
                this.favoriteProductRepository = this@GdprServiceTest.favoriteProductRepository
                this.pointTransactionRepository = this@GdprServiceTest.pointTransactionRepository
                this.giftCardRepository = this@GdprServiceTest.giftCardRepository
                this.webhookRepository = this@GdprServiceTest.webhookRepository
                this.subscriptionRepository = this@GdprServiceTest.subscriptionRepository
                this.systemSettingRepository = this@GdprServiceTest.systemSettingRepository
                this.auditLogRepository = this@GdprServiceTest.auditLogRepository
                this.consentRepository = this@GdprServiceTest.consentRepository
            }
    }

    // === deleteOrganizationData ===

    @Nested
    inner class DeleteOrganizationData {
        @Test
        fun `正常にテナントデータを削除しPIIを匿名化する`() {
            // Arrange
            val org =
                OrganizationEntity().apply {
                    id = orgId
                    name = "テスト組織"
                    businessType = "RETAIL"
                }
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(org)

            val staff =
                StaffEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    storeId = UUID.randomUUID()
                    name = "田中太郎"
                    email = "tanaka@example.com"
                    pinHash = "hashed_pin"
                    hydraSubject = "subject_123"
                }
            whenever(staffRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(staff))

            val customer =
                CustomerEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    name = "山田花子"
                    email = "yamada@example.com"
                    phone = "090-1234-5678"
                }
            whenever(customerRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(customer))

            val store =
                StoreEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    name = "本店"
                    address = "東京都渋谷区"
                    phone = "03-1234-5678"
                }
            whenever(storeRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(store))

            val auditLog =
                AuditLogEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    action = "CREATE"
                    entityType = "STAFF"
                    ipAddress = "192.168.1.***"
                }
            whenever(auditLogRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(auditLog))

            // Act
            val result = service.deleteOrganizationData(orgId)

            // Assert
            assertTrue(result)
            // スタッフの PII が匿名化されている
            assertEquals(GdprService.ANONYMIZED_NAME, staff.name)
            assertNull(staff.email)
            assertNull(staff.pinHash)
            assertNull(staff.hydraSubject)
            assertFalse(staff.isActive)
            // 顧客の PII が匿名化されている
            assertEquals(GdprService.ANONYMIZED_NAME, customer.name)
            assertNull(customer.email)
            assertNull(customer.phone)
            assertFalse(customer.isActive)
            // 店舗の PII が匿名化されている
            assertNull(store.address)
            assertNull(store.phone)
            assertFalse(store.isActive)
            // 監査ログの IP がクリアされている
            assertNull(auditLog.ipAddress)
            // 組織が論理削除されている
            assertNotNull(org.deletedAt)
        }

        @Test
        fun `存在しない組織の場合はIllegalArgumentExceptionをスローする`() {
            // Arrange
            whenever(organizationRepository.findByIdNotDeleted(orgId)).thenReturn(null)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                service.deleteOrganizationData(orgId)
            }
        }
    }

    // === anonymizeStaffData ===

    @Nested
    inner class AnonymizeStaffData {
        @Test
        fun `スタッフのPIIを匿名化して件数を返す`() {
            // Arrange
            val staff1 =
                StaffEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    storeId = UUID.randomUUID()
                    name = "鈴木一郎"
                    email = "suzuki@example.com"
                    pinHash = "hash1"
                    hydraSubject = "sub1"
                }
            val staff2 =
                StaffEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    storeId = UUID.randomUUID()
                    name = "佐藤二郎"
                    email = "sato@example.com"
                    pinHash = "hash2"
                    hydraSubject = "sub2"
                }
            whenever(staffRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(staff1, staff2))

            // Act
            val count = service.anonymizeStaffData(orgId)

            // Assert
            assertEquals(2, count)
            assertEquals(GdprService.ANONYMIZED_NAME, staff1.name)
            assertNull(staff1.email)
            assertNull(staff1.pinHash)
            assertEquals(GdprService.ANONYMIZED_NAME, staff2.name)
            assertNull(staff2.email)
        }

        @Test
        fun `スタッフが存在しない場合は0を返す`() {
            // Arrange
            whenever(staffRepository.findAllByOrganizationId(orgId))
                .thenReturn(emptyList())

            // Act
            val count = service.anonymizeStaffData(orgId)

            // Assert
            assertEquals(0, count)
        }
    }

    // === anonymizeCustomerData ===

    @Nested
    inner class AnonymizeCustomerData {
        @Test
        fun `顧客のPIIを匿名化して件数を返す`() {
            // Arrange
            val customer =
                CustomerEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    name = "テスト顧客"
                    email = "customer@example.com"
                    phone = "080-9876-5432"
                }
            whenever(customerRepository.findAllByOrganizationId(orgId))
                .thenReturn(listOf(customer))

            // Act
            val count = service.anonymizeCustomerData(orgId)

            // Assert
            assertEquals(1, count)
            assertEquals(GdprService.ANONYMIZED_NAME, customer.name)
            assertNull(customer.email)
            assertNull(customer.phone)
        }
    }

    // === recordConsent ===

    @Nested
    inner class RecordConsent {
        @Test
        fun `新規同意を記録する`() {
            // Arrange
            whenever(consentRepository.findByOrganizationAndType(orgId, "DATA_PROCESSING"))
                .thenReturn(null)
            doNothing().whenever(consentRepository).persist(any<DataProcessingConsentEntity>())

            // Act
            val result =
                service.recordConsent(
                    organizationId = orgId,
                    consentType = "DATA_PROCESSING",
                    granted = true,
                    grantedBy = UUID.randomUUID(),
                    policyVersion = "1.0",
                    ipAddress = "192.168.1.***",
                )

            // Assert
            assertEquals(orgId, result.organizationId)
            assertEquals("DATA_PROCESSING", result.consentType)
            assertTrue(result.granted)
            assertNotNull(result.grantedAt)
            assertNull(result.revokedAt)
            assertEquals("1.0", result.policyVersion)
            verify(consentRepository).persist(any<DataProcessingConsentEntity>())
        }

        @Test
        fun `既存同意を撤回する`() {
            // Arrange
            val existing =
                DataProcessingConsentEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    consentType = "MARKETING"
                    granted = true
                    policyVersion = "1.0"
                }
            whenever(consentRepository.findByOrganizationAndType(orgId, "MARKETING"))
                .thenReturn(existing)
            doNothing().whenever(consentRepository).persist(any<DataProcessingConsentEntity>())

            // Act
            val result =
                service.recordConsent(
                    organizationId = orgId,
                    consentType = "MARKETING",
                    granted = false,
                    grantedBy = null,
                    policyVersion = "1.1",
                    ipAddress = null,
                )

            // Assert
            assertFalse(result.granted)
            assertNotNull(result.revokedAt)
            assertEquals("1.1", result.policyVersion)
        }
    }

    // === getConsents ===

    @Nested
    inner class GetConsents {
        @Test
        fun `組織の全同意を取得する`() {
            // Arrange
            val consent1 =
                DataProcessingConsentEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    consentType = "DATA_PROCESSING"
                    granted = true
                    policyVersion = "1.0"
                }
            val consent2 =
                DataProcessingConsentEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    consentType = "MARKETING"
                    granted = false
                    policyVersion = "1.0"
                }
            whenever(consentRepository.findByOrganizationId(orgId))
                .thenReturn(listOf(consent1, consent2))

            // Act
            val result = service.getConsents(orgId)

            // Assert
            assertEquals(2, result.size)
        }

        @Test
        fun `同意が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(consentRepository.findByOrganizationId(orgId))
                .thenReturn(emptyList())

            // Act
            val result = service.getConsents(orgId)

            // Assert
            assertTrue(result.isEmpty())
        }
    }

    // === getConsent ===

    @Nested
    inner class GetConsent {
        @Test
        fun `特定種別の同意を取得する`() {
            // Arrange
            val consent =
                DataProcessingConsentEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    consentType = "DATA_PROCESSING"
                    granted = true
                    policyVersion = "1.0"
                }
            whenever(consentRepository.findByOrganizationAndType(orgId, "DATA_PROCESSING"))
                .thenReturn(consent)

            // Act
            val result = service.getConsent(orgId, "DATA_PROCESSING")

            // Assert
            assertNotNull(result)
            assertEquals("DATA_PROCESSING", result?.consentType)
            assertTrue(result?.granted == true)
        }

        @Test
        fun `存在しない種別の場合はnullを返す`() {
            // Arrange
            whenever(consentRepository.findByOrganizationAndType(orgId, "UNKNOWN"))
                .thenReturn(null)

            // Act
            val result = service.getConsent(orgId, "UNKNOWN")

            // Assert
            assertNull(result)
        }
    }
}
