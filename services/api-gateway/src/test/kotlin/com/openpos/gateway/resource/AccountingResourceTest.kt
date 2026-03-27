package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class AccountingResourceTest {
    private fun createResource(role: String? = null): AccountingResource {
        val ctx = TenantContext()
        ctx.organizationId = UUID.randomUUID()
        ctx.staffRole = role
        return AccountingResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", ctx)
        }
    }

    @Nested
    inner class ExportDailySales {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.exportDailySales("2026-03-27")

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはアクセス不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.exportDailySales("2026-03-27")
            }
        }

        @Test
        fun `CASHIERはアクセス不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.exportDailySales("2026-03-27")
            }
        }
    }

    @Nested
    inner class ExportTransactions {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.exportTransactions("2026-03-01", "2026-03-27")

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはアクセス不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.exportTransactions("2026-03-01", "2026-03-27")
            }
        }

        @Test
        fun `CASHIERはアクセス不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.exportTransactions("2026-03-01", "2026-03-27")
            }
        }
    }
}
