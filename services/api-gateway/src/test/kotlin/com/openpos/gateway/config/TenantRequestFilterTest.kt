package com.openpos.gateway.config

import jakarta.ws.rs.container.ContainerRequestContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class TenantRequestFilterTest {
    private val tenantContext = TenantContext()
    private val filter =
        TenantRequestFilter().also {
            val field = TenantRequestFilter::class.java.getDeclaredField("tenantContext")
            field.isAccessible = true
            field.set(it, tenantContext)
        }
    private val requestContext: ContainerRequestContext = mock()

    @BeforeEach
    fun setUp() {
        tenantContext.organizationId = null
    }

    @Nested
    inner class ValidHeader {
        @Test
        fun `有効なUUIDヘッダーでorganizationIdが設定される`() {
            // Arrange
            val orgId = UUID.randomUUID()
            whenever(requestContext.getHeaderString("X-Organization-Id")).thenReturn(orgId.toString())

            // Act
            filter.filter(requestContext)

            // Assert
            assertEquals(orgId, tenantContext.organizationId)
        }
    }

    @Nested
    inner class MissingHeader {
        @Test
        fun `ヘッダーが存在しない場合はorganizationIdがnullのまま`() {
            // Arrange
            whenever(requestContext.getHeaderString("X-Organization-Id")).thenReturn(null)

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(tenantContext.organizationId)
        }
    }

    @Nested
    inner class InvalidHeader {
        @Test
        fun `不正なUUID文字列の場合はorganizationIdがnullのまま`() {
            // Arrange
            whenever(requestContext.getHeaderString("X-Organization-Id")).thenReturn("not-a-uuid")

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(tenantContext.organizationId)
        }

        @Test
        fun `空文字の場合はorganizationIdがnullのまま`() {
            // Arrange
            whenever(requestContext.getHeaderString("X-Organization-Id")).thenReturn("")

            // Act
            filter.filter(requestContext)

            // Assert
            assertNull(tenantContext.organizationId)
        }
    }
}
