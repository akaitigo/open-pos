package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
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
        fun `MANAGERはアクセス不可`() {
            val resource = createResource("MANAGER")
            assertThrows<ForbiddenException> {
                resource.exportDailySales(storeId = "store-1", date = "2026-03-27")
            }
        }

        @Test
        fun `CASHIERはアクセス不可`() {
            val resource = createResource("CASHIER")
            assertThrows<ForbiddenException> {
                resource.exportDailySales(storeId = "store-1", date = "2026-03-27")
            }
        }
    }

    @Nested
    inner class ExportTransactions {
        @Test
        fun `MANAGERはアクセス不可`() {
            val resource = createResource("MANAGER")
            assertThrows<ForbiddenException> {
                resource.exportTransactions(storeId = "store-1", startDate = "2026-03-01", endDate = "2026-03-27")
            }
        }

        @Test
        fun `CASHIERはアクセス不可`() {
            val resource = createResource("CASHIER")
            assertThrows<ForbiddenException> {
                resource.exportTransactions(storeId = "store-1", startDate = "2026-03-01", endDate = "2026-03-27")
            }
        }
    }
}
