package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class DiscountReasonResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        DiscountReasonResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @Nested
    inner class List {
        @Test
        fun `一覧取得で501を返す`() {
            val response = resource.list()
            assertEquals(501, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals("NOT_IMPLEMENTED", entity["error"])
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `作成で501を返す`() {
            val body = CreateDiscountReasonBody(code = "EMPLOYEE", description = "従業員割引")
            val response = resource.create(body)
            assertEquals(501, response.status)
        }

        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body = CreateDiscountReasonBody(code = "EMPLOYEE", description = "従業員割引")
            assertThrows<ForbiddenException> { resource.create(body) }
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `更新で501を返す`() {
            val body = UpdateDiscountReasonBody(description = "更新された説明")
            val response = resource.update("some-id", body)
            assertEquals(501, response.status)
        }

        @Test
        fun `STAFF権限で更新するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body = UpdateDiscountReasonBody(description = "更新された説明")
            assertThrows<ForbiddenException> { resource.update("some-id", body) }
        }
    }
}
