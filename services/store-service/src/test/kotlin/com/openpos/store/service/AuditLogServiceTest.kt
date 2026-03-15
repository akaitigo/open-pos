package com.openpos.store.service

import com.openpos.store.entity.AuditLogEntity
import com.openpos.store.repository.AuditLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class AuditLogServiceTest {
    private val auditLogRepository: AuditLogRepository = mock()
    private val auditLogService =
        AuditLogService().also {
            val field = AuditLogService::class.java.getDeclaredField("auditLogRepository")
            field.isAccessible = true
            field.set(it, auditLogRepository)
        }

    private val testOrgId = UUID.randomUUID()
    private val testStaffId = UUID.randomUUID()

    @Nested
    inner class LogMethod {
        @Test
        fun `監査ログを正しく記録する`() {
            // Arrange
            val captor = argumentCaptor<AuditLogEntity>()

            // Act
            auditLogService.log(
                organizationId = testOrgId,
                staffId = testStaffId,
                action = "CREATE",
                entityType = "STAFF",
                entityId = "entity-123",
                details = """{"name":"test"}""",
                ipAddress = "192.168.1.100",
            )

            // Assert
            verify(auditLogRepository).persist(captor.capture())
            val entity = captor.firstValue
            assertEquals(testOrgId, entity.organizationId)
            assertEquals(testStaffId, entity.staffId)
            assertEquals("CREATE", entity.action)
            assertEquals("STAFF", entity.entityType)
            assertEquals("entity-123", entity.entityId)
            assertEquals("""{"name":"test"}""", entity.details)
            // IP はマスクされている
            assertEquals("192.168.1.***", entity.ipAddress)
        }

        @Test
        fun `staffIdがnullでも記録できる`() {
            // Arrange
            val captor = argumentCaptor<AuditLogEntity>()

            // Act
            auditLogService.log(
                organizationId = testOrgId,
                action = "SYSTEM_EVENT",
                entityType = "STORE",
            )

            // Assert
            verify(auditLogRepository).persist(captor.capture())
            val entity = captor.firstValue
            assertEquals(testOrgId, entity.organizationId)
            assertEquals(null, entity.staffId)
            assertEquals("SYSTEM_EVENT", entity.action)
        }

        @Test
        fun `IPv4アドレスの最終オクテットがマスクされる`() {
            // Arrange
            val captor = argumentCaptor<AuditLogEntity>()

            // Act
            auditLogService.log(
                organizationId = testOrgId,
                action = "LOGIN_SUCCESS",
                entityType = "STAFF",
                ipAddress = "10.0.0.1",
            )

            // Assert
            verify(auditLogRepository).persist(captor.capture())
            assertEquals("10.0.0.***", captor.firstValue.ipAddress)
        }

        @Test
        fun `nullのIPアドレスはnullのまま`() {
            // Arrange
            val captor = argumentCaptor<AuditLogEntity>()

            // Act
            auditLogService.log(
                organizationId = testOrgId,
                action = "CREATE",
                entityType = "PRODUCT",
                ipAddress = null,
            )

            // Assert
            verify(auditLogRepository).persist(captor.capture())
            assertEquals(null, captor.firstValue.ipAddress)
        }

        @Test
        fun `createdAtが自動設定される`() {
            // Arrange
            val captor = argumentCaptor<AuditLogEntity>()

            // Act
            auditLogService.log(
                organizationId = testOrgId,
                action = "DELETE",
                entityType = "PRODUCT",
                entityId = "prod-1",
            )

            // Assert
            verify(auditLogRepository).persist(captor.capture())
            assertNotNull(captor.firstValue.createdAt)
        }
    }
}
