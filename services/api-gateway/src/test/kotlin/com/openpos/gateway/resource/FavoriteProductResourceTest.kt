package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class FavoriteProductResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        FavoriteProductResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @Nested
    inner class List {
        @Test
        fun `お気に入り一覧取得で501を返す`() {
            val response = resource.list("staff-1")
            assertEquals(501, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals("NOT_IMPLEMENTED", entity["error"])
        }
    }

    @Nested
    inner class Add {
        @Test
        fun `お気に入り追加で501を返す`() {
            val body = AddFavoriteBody(productId = "prod-1")
            val response = resource.add("staff-1", body)
            assertEquals(501, response.status)
        }
    }

    @Nested
    inner class Remove {
        @Test
        fun `お気に入り削除で501を返す`() {
            val response = resource.remove("staff-1", "prod-1")
            assertEquals(501, response.status)
        }
    }
}
