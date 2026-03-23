package com.openpos.store.service

import com.openpos.store.entity.AuditLogEntity
import com.openpos.store.repository.AuditLogRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import java.util.UUID

class AuditLogServiceUnitTest {
    private val auditLogRepository: AuditLogRepository = mock()
    private val service =
        AuditLogService().also {
            it.auditLogRepository = auditLogRepository
        }

    private val orgId = UUID.randomUUID()

    @Nested
    inner class MaskIpAddress {
        @Test
        fun `IPv4アドレスの最終オクテットがマスクされる`() {
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(organizationId = orgId, action = "TEST", entityType = "T", ipAddress = "192.168.1.100")

            verify(auditLogRepository).persist(captor.capture())
            assertEquals("192.168.1.***", captor.firstValue.ipAddress)
        }

        @Test
        fun `IPv6アドレスの最終セグメントがマスクされる`() {
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(organizationId = orgId, action = "TEST", entityType = "T", ipAddress = "2001:0db8:85a3:0000:0000:8a2e:0370:7334")

            verify(auditLogRepository).persist(captor.capture())
            assertEquals("2001:0db8:85a3:0000:0000:8a2e:0370:***", captor.firstValue.ipAddress)
        }

        @Test
        fun `nullのIPアドレスはnullのまま`() {
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(organizationId = orgId, action = "TEST", entityType = "T", ipAddress = null)

            verify(auditLogRepository).persist(captor.capture())
            assertNull(captor.firstValue.ipAddress)
        }

        @Test
        fun `コロンなしの非IPv4文字列はそのまま返される`() {
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(organizationId = orgId, action = "TEST", entityType = "T", ipAddress = "unknown-format")

            verify(auditLogRepository).persist(captor.capture())
            assertEquals("unknown-format", captor.firstValue.ipAddress)
        }
    }

    @Nested
    inner class LogMethod {
        @Test
        fun `全フィールドが正しく設定される`() {
            val staffId = UUID.randomUUID()
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(
                organizationId = orgId,
                staffId = staffId,
                action = "CREATE",
                entityType = "STAFF",
                entityId = "entity-123",
                details = """{"name":"test"}""",
                ipAddress = "10.0.0.1",
            )

            verify(auditLogRepository).persist(captor.capture())
            val entity = captor.firstValue
            assertEquals(orgId, entity.organizationId)
            assertEquals(staffId, entity.staffId)
            assertEquals("CREATE", entity.action)
            assertEquals("STAFF", entity.entityType)
            assertEquals("entity-123", entity.entityId)
            assertEquals("""{"name":"test"}""", entity.details)
            assertEquals("10.0.0.***", entity.ipAddress)
            assertNotNull(entity.createdAt)
        }

        @Test
        fun `デフォルト値で記録できる`() {
            val captor = argumentCaptor<AuditLogEntity>()

            service.log(organizationId = orgId, action = "SYSTEM", entityType = "STORE")

            verify(auditLogRepository).persist(captor.capture())
            val entity = captor.firstValue
            assertNull(entity.staffId)
            assertNull(entity.entityId)
            assertEquals("{}", entity.details)
            assertNull(entity.ipAddress)
        }
    }
}
