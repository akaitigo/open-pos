package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiscountReasonResourceTest {
    private val tenantContext =
        TenantContext().apply {
            staffRole = "STAFF"
        }

    private val resource =
        DiscountReasonResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @Nested
    inner class Create {
        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            val body = CreateDiscountReasonBody(code = "EMPLOYEE", description = "従業員割引")
            assertThrows<ForbiddenException> { resource.create(body) }
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `STAFF権限で更新するとForbiddenException`() {
            val body = UpdateDiscountReasonBody(description = "更新された説明")
            assertThrows<ForbiddenException> { resource.update("some-id", body) }
        }
    }
}
