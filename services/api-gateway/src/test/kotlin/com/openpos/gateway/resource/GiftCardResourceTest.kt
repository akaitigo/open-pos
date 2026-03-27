package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GiftCardResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        GiftCardResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
    }

    @Nested
    inner class ListGiftCards {
        @Test
        fun `一覧取得で空リストとページネーションを返す`() {
            // Act
            val result = resource.list(page = 1, pageSize = 20)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(0, data.size)
            @Suppress("UNCHECKED_CAST")
            val pagination = result["pagination"] as Map<String, Any>
            assertEquals(1, pagination["page"])
            assertEquals(20, pagination["pageSize"])
            assertEquals(0, pagination["totalCount"])
        }

        @Test
        fun `カスタムページサイズで一覧取得`() {
            // Act
            val result = resource.list(page = 2, pageSize = 10)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val pagination = result["pagination"] as Map<String, Any>
            assertEquals(2, pagination["page"])
            assertEquals(10, pagination["pageSize"])
        }
    }

    @Nested
    inner class GetGiftCard {
        @Test
        fun `存在しないギフトカードで404を返す`() {
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
    inner class CreateGiftCard {
        @Test
        fun `ギフトカード発行で201を返す`() {
            // Arrange
            val body = CreateGiftCardBody(initialAmount = 500000)

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals(500000L, entity["initialAmount"])
            assertEquals(500000L, entity["balance"])
            assertEquals("ACTIVE", entity["status"])
            assertNotNull(entity["id"])
            assertNotNull(entity["code"])
        }

        @Test
        fun `有効期限付きギフトカード発行`() {
            // Arrange
            val body = CreateGiftCardBody(initialAmount = 300000, expiresAt = "2027-03-01T00:00:00Z")

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("2027-03-01T00:00:00Z", entity["expiresAt"])
        }

        @Test
        fun `STAFF権限で発行するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"
            val body = CreateGiftCardBody(initialAmount = 500000)

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.create(body)
            }
        }

        @Test
        fun `MANAGER権限で発行可能`() {
            // Arrange
            tenantContext.staffRole = "MANAGER"
            val body = CreateGiftCardBody(initialAmount = 500000)

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class ActivateGiftCard {
        @Test
        fun `ギフトカード有効化で200を返す`() {
            // Act
            val response = resource.activate("ABCD-1234-EFGH-5678")

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("ABCD-1234-EFGH-5678", entity["code"])
            assertEquals("ACTIVE", entity["status"])
            assertNotNull(entity["activatedAt"])
        }

        @Test
        fun `STAFF権限で有効化可能`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act
            val response = resource.activate("ABCD-1234-EFGH-5678")

            // Assert
            assertEquals(200, response.status)
        }
    }

    @Nested
    inner class RedeemGiftCard {
        @Test
        fun `ギフトカード利用で200を返す`() {
            // Arrange
            val body = RedeemGiftCardBody(amount = 100000)

            // Act
            val response = resource.redeem("ABCD-1234-EFGH-5678", body)

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("ABCD-1234-EFGH-5678", entity["code"])
            assertEquals(100000L, entity["redeemedAmount"])
        }

        @Test
        fun `0円利用でIllegalArgumentException`() {
            // Arrange
            val body = RedeemGiftCardBody(amount = 0)

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.redeem("ABCD-1234-EFGH-5678", body)
            }
        }

        @Test
        fun `負の金額利用でIllegalArgumentException`() {
            // Arrange
            val body = RedeemGiftCardBody(amount = -100)

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.redeem("ABCD-1234-EFGH-5678", body)
            }
        }
    }

    @Nested
    inner class CheckBalance {
        @Test
        fun `存在しないカードの残高確認で404を返す`() {
            // Act
            val response = resource.checkBalance("NONEXISTENT")

            // Assert
            assertEquals(404, response.status)
        }
    }
}
