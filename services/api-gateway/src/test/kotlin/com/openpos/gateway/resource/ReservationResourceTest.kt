package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
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
    inner class List {
        @Test
        fun `全ロールで501を返す`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act
            val response = resource.list(storeId = null, status = null, page = 1, pageSize = 20)

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.list(storeId = "store-1", status = "PENDING", page = 1, pageSize = 10)

            // Assert
            assertEquals(501, response.status)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")
            val body =
                CreateReservationBody(
                    storeId = "store-1",
                    customerName = "テスト太郎",
                    reservedUntil = "2026-04-01T10:00:00Z",
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERで501を返す`() {
            // Arrange
            val resource = createResource("MANAGER")
            val body =
                CreateReservationBody(
                    storeId = "store-1",
                    reservedUntil = "2026-04-01T10:00:00Z",
                    items = listOf(ReservationItemBody(productId = "prod-1", quantity = 2)),
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `CASHIERは作成不可`() {
            // Arrange
            val resource = createResource("CASHIER")
            val body =
                CreateReservationBody(
                    storeId = "store-1",
                    reservedUntil = "2026-04-01T10:00:00Z",
                )

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.create(body)
            }
        }
    }

    @Nested
    inner class Fulfill {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.fulfill("reservation-1")

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `CASHIERは履行不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.fulfill("reservation-1")
            }
        }
    }

    @Nested
    inner class Cancel {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.cancel("reservation-1")

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERで501を返す`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act
            val response = resource.cancel("reservation-1")

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `CASHIERはキャンセル不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.cancel("reservation-1")
            }
        }
    }
}
