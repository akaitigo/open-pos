package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class ReservationResourceTest {
    private fun createResource(role: String? = null): ReservationResource {
        val ctx = TenantContext()
        ctx.organizationId = UUID.randomUUID()
        ctx.staffRole = role
        return ReservationResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", ctx)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `CASHIERは作成不可`() {
            val resource = createResource("CASHIER")
            val body =
                CreateReservationBody(
                    storeId = "store-1",
                    reservedUntil = "2026-04-01T10:00:00Z",
                )
            assertThrows<ForbiddenException> {
                resource.create(body)
            }
        }
    }

    @Nested
    inner class Fulfill {
        @Test
        fun `CASHIERは履行不可`() {
            val resource = createResource("CASHIER")
            assertThrows<ForbiddenException> {
                resource.fulfill("reservation-1")
            }
        }
    }

    @Nested
    inner class Cancel {
        @Test
        fun `CASHIERはキャンセル不可`() {
            val resource = createResource("CASHIER")
            assertThrows<ForbiddenException> {
                resource.cancel("reservation-1")
            }
        }
    }
}
