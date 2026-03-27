package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class StampCardResourceTest {
    private val tenantContext = TenantContext()
    private val resource =
        StampCardResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
    }

    @Nested
    inner class GetStampCard {
        @Test
        fun `存在しないスタンプカードで404を返す`() {
            // Act
            val response = resource.get("customer-123")

            // Assert
            assertEquals(404, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any>
            assertEquals("NOT_FOUND", entity["error"])
        }
    }

    @Nested
    inner class IssueStampCard {
        @Test
        fun `スタンプカード発行で201を返す`() {
            // Arrange
            val body = IssueStampCardBody(customerId = "customer-123", maxStamps = 10)

            // Act
            val response = resource.issue(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("customer-123", entity["customerId"])
            assertEquals(0, entity["stampCount"])
            assertEquals(10, entity["maxStamps"])
            assertEquals("ACTIVE", entity["status"])
            assertNotNull(entity["id"])
        }

        @Test
        fun `報酬説明付きスタンプカード発行`() {
            // Arrange
            val body =
                IssueStampCardBody(
                    customerId = "customer-456",
                    maxStamps = 5,
                    rewardDescription = "ドリンク1杯無料",
                )

            // Act
            val response = resource.issue(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("ドリンク1杯無料", entity["rewardDescription"])
            assertEquals(5, entity["maxStamps"])
        }

        @Test
        fun `STAFF権限で発行可能`() {
            // Arrange
            tenantContext.staffRole = "STAFF"
            val body = IssueStampCardBody(customerId = "customer-123")

            // Act
            val response = resource.issue(body)

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class AddStamp {
        @Test
        fun `スタンプ追加で200を返す`() {
            // Act
            val response = resource.addStamp("customer-123")

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("customer-123", entity["customerId"])
            assertEquals(1, entity["stampCount"])
            assertNotNull(entity["stampedAt"])
        }

        @Test
        fun `STAFF権限でスタンプ追加可能`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act
            val response = resource.addStamp("customer-123")

            // Assert
            assertEquals(200, response.status)
        }
    }

    @Nested
    inner class RedeemReward {
        @Test
        fun `スタンプ報酬交換で200を返す`() {
            // Act
            val response = resource.redeemReward("customer-123")

            // Assert
            assertEquals(200, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals("customer-123", entity["customerId"])
            assertEquals(true, entity["redeemed"])
            assertEquals(0, entity["stampCount"])
            assertNotNull(entity["redeemedAt"])
        }

        @Test
        fun `STAFF権限で報酬交換可能`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act
            val response = resource.redeemReward("customer-123")

            // Assert
            assertEquals(200, response.status)
        }
    }
}
