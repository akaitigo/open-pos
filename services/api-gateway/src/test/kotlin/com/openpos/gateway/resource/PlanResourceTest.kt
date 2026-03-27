package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.TenantContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.UUID

class PlanResourceTest {
    private fun createResource(role: String? = null): PlanResource {
        val ctx = TenantContext()
        ctx.organizationId = UUID.randomUUID()
        ctx.staffRole = role
        return PlanResource().also { r ->
            ProductResourceTest.setField(r, "tenantContext", ctx)
        }
    }

    @Nested
    inner class ListPlans {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.listPlans()

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはアクセス不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.listPlans()
            }
        }

        @Test
        fun `CASHIERはアクセス不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.listPlans()
            }
        }
    }

    @Nested
    inner class GetCurrentPlan {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.getCurrentPlan()

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはアクセス不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.getCurrentPlan()
            }
        }
    }

    @Nested
    inner class ChangePlan {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.changePlan(ChangePlanBody(planId = "plan-pro"))

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはプラン変更不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.changePlan(ChangePlanBody(planId = "plan-pro"))
            }
        }

        @Test
        fun `CASHIERはプラン変更不可`() {
            // Arrange
            val resource = createResource("CASHIER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.changePlan(ChangePlanBody(planId = "plan-pro"))
            }
        }
    }

    @Nested
    inner class Subscribe {
        @Test
        fun `OWNERで501を返す`() {
            // Arrange
            val resource = createResource("OWNER")

            // Act
            val response = resource.subscribe(SubscribeBody(planId = "plan-starter"))

            // Assert
            assertEquals(501, response.status)
        }

        @Test
        fun `MANAGERはサブスクライブ不可`() {
            // Arrange
            val resource = createResource("MANAGER")

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.subscribe(SubscribeBody(planId = "plan-starter"))
            }
        }
    }
}
