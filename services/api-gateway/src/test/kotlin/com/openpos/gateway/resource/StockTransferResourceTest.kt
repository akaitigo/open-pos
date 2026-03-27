package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StockTransferResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        StockTransferResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @Nested
    inner class List {
        @Test
        fun `一覧取得で501を返す`() {
            val response = resource.list(page = 1, pageSize = 20)
            assertEquals(501, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals("NOT_IMPLEMENTED", entity["error"])
        }

        @Test
        fun `STAFF権限で一覧取得するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            assertThrows<ForbiddenException> { resource.list(page = 1, pageSize = 20) }
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `作成で501を返す`() {
            val body = CreateStockTransferBody(
                fromStoreId = "store-1",
                toStoreId = "store-2",
                items = listOf(StockTransferItemBody(productId = "prod-1", quantity = 10)),
            )
            val response = resource.create(body)
            assertEquals(501, response.status)
        }

        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body = CreateStockTransferBody(
                fromStoreId = "store-1",
                toStoreId = "store-2",
                items = listOf(StockTransferItemBody(productId = "prod-1", quantity = 10)),
            )
            assertThrows<ForbiddenException> { resource.create(body) }
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `取得で501を返す`() {
            val response = resource.get("transfer-id")
            assertEquals(501, response.status)
        }

        @Test
        fun `STAFF権限で取得するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            assertThrows<ForbiddenException> { resource.get("transfer-id") }
        }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `ステータス更新で501を返す`() {
            val body = UpdateStockTransferStatusBody(status = "SHIPPED")
            val response = resource.updateStatus("transfer-id", body)
            assertEquals(501, response.status)
        }

        @Test
        fun `STAFF権限でステータス更新するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body = UpdateStockTransferStatusBody(status = "SHIPPED")
            assertThrows<ForbiddenException> { resource.updateStatus("transfer-id", body) }
        }
    }
}
