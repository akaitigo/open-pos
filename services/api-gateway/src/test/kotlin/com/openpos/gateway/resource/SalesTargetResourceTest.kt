package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SalesTargetResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        SalesTargetResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
    }

    @Nested
    inner class ListSalesTargets {
        @Test
        fun `一覧取得で空リストとフィルターを返す`() {
            // Act
            val result = resource.list(storeId = null, month = null)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(0, data.size)
        }

        @Test
        fun `フィルター付き一覧取得`() {
            // Act
            val result = resource.list(storeId = "store-1", month = "2026-03")

            // Assert
            @Suppress("UNCHECKED_CAST")
            val filters = result["filters"] as Map<String, Any?>
            assertEquals("store-1", filters["storeId"])
            assertEquals("2026-03", filters["month"])
        }

        @Test
        fun `STAFF権限で一覧取得するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.list(storeId = null, month = null)
            }
        }

        @Test
        fun `MANAGER権限で一覧取得可能`() {
            // Arrange
            tenantContext.staffRole = "MANAGER"

            // Act
            val result = resource.list(storeId = null, month = null)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(0, data.size)
        }
    }

    @Nested
    inner class GetSalesTarget {
        @Test
        fun `存在しない売上目標で404を返す`() {
            // Act
            val response = resource.get("nonexistent-id")

            // Assert
            assertEquals(404, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals("NOT_FOUND", entity["error"])
        }
    }

    @Nested
    inner class CreateSalesTarget {
        @Test
        fun `売上目標作成で201を返す`() {
            // Arrange
            val body =
                CreateSalesTargetBody(
                    storeId = "store-1",
                    targetMonth = "2026-04",
                    targetAmount = 100000000,
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("store-1", entity["storeId"])
            assertEquals("2026-04", entity["targetMonth"])
            assertEquals(100000000L, entity["targetAmount"])
            assertEquals(0L, entity["currentAmount"])
            assertNotNull(entity["id"])
        }

        @Test
        fun `スタッフ指定で売上目標作成`() {
            // Arrange
            val body =
                CreateSalesTargetBody(
                    storeId = "store-1",
                    staffId = "staff-1",
                    targetMonth = "2026-04",
                    targetAmount = 50000000,
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("staff-1", entity["staffId"])
        }

        @Test
        fun `0円以下の目標額でIllegalArgumentException`() {
            // Arrange
            val body =
                CreateSalesTargetBody(
                    storeId = "store-1",
                    targetMonth = "2026-04",
                    targetAmount = 0,
                )

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.create(body)
            }
        }

        @Test
        fun `負の目標額でIllegalArgumentException`() {
            // Arrange
            val body =
                CreateSalesTargetBody(
                    storeId = "store-1",
                    targetMonth = "2026-04",
                    targetAmount = -100,
                )

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.create(body)
            }
        }

        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"
            val body =
                CreateSalesTargetBody(
                    storeId = "store-1",
                    targetMonth = "2026-04",
                    targetAmount = 100000000,
                )

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.create(body)
            }
        }
    }

    @Nested
    inner class UpdateSalesTarget {
        @Test
        fun `更新で404を返す`() {
            // Arrange
            val body = UpdateSalesTargetBody(targetAmount = 200000000)

            // Act
            val response = resource.update("some-id", body)

            // Assert
            assertEquals(404, response.status)
        }

        @Test
        fun `0円以下の更新額でIllegalArgumentException`() {
            // Arrange
            val body = UpdateSalesTargetBody(targetAmount = 0)

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.update("some-id", body)
            }
        }
    }

    @Nested
    inner class DeleteSalesTarget {
        @Test
        fun `削除で204を返す`() {
            // Act
            val response = resource.delete("some-id")

            // Assert
            assertEquals(204, response.status)
        }

        @Test
        fun `STAFF権限で削除するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.delete("some-id")
            }
        }
    }
}
