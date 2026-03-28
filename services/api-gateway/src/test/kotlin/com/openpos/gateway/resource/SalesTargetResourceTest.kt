package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import io.grpc.Status
import io.grpc.StatusRuntimeException
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.DeleteSalesTargetResponse
import openpos.analytics.v1.GetSalesTargetResponse
import openpos.analytics.v1.ListSalesTargetsResponse
import openpos.analytics.v1.SalesTarget
import openpos.analytics.v1.UpsertSalesTargetResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class SalesTargetResourceTest {
    private val stub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext: TenantContext = TenantContext()
    private val resource =
        SalesTargetResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val targetId = UUID.randomUUID().toString()

    private fun buildSalesTarget(id: String = targetId): SalesTarget =
        SalesTarget
            .newBuilder()
            .setId(id)
            .setOrganizationId(orgId)
            .setStoreId(UUID.randomUUID().toString())
            .setTargetMonth("2026-04-01")
            .setTargetAmount(10000000)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
        tenantContext.staffRole = null
    }

    @Nested
    inner class ListSalesTargets {
        @Test
        fun `一覧取得でデータリストを返す`() {
            // Arrange
            val target = buildSalesTarget()
            whenever(stub.listSalesTargets(any())).thenReturn(
                ListSalesTargetsResponse.newBuilder().addSalesTargets(target).build(),
            )

            // Act
            val result = resource.list(storeId = null, month = null)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(1, data.size)
        }

        @Test
        fun `フィルター付き一覧取得`() {
            // Arrange
            whenever(stub.listSalesTargets(any())).thenReturn(
                ListSalesTargetsResponse.newBuilder().build(),
            )

            // Act
            val result = resource.list(storeId = "store-1", month = "2026-04-01")

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(0, data.size)
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
            whenever(stub.listSalesTargets(any())).thenReturn(
                ListSalesTargetsResponse.newBuilder().build(),
            )

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
        fun `IDで売上目標を取得`() {
            // Arrange
            val target = buildSalesTarget()
            whenever(stub.getSalesTarget(any())).thenReturn(
                GetSalesTargetResponse.newBuilder().setSalesTarget(target).build(),
            )

            // Act
            val result = resource.get(targetId)

            // Assert
            assertEquals(targetId, result["id"])
            assertEquals(10000000L, result["targetAmount"])
        }

        @Test
        fun `存在しない売上目標でNOT_FOUND例外`() {
            // Arrange
            whenever(stub.getSalesTarget(any())).thenThrow(
                StatusRuntimeException(Status.NOT_FOUND.withDescription("Sales target not found")),
            )

            // Act & Assert
            assertThrows<StatusRuntimeException> {
                resource.get("nonexistent-id")
            }
        }

        @Test
        fun `STAFF権限で取得するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.get(targetId)
            }
        }
    }

    @Nested
    inner class UpsertSalesTarget {
        @Test
        fun `売上目標を作成・更新で200を返す`() {
            // Arrange
            val target = buildSalesTarget()
            whenever(stub.upsertSalesTarget(any())).thenReturn(
                UpsertSalesTargetResponse.newBuilder().setSalesTarget(target).build(),
            )
            val body =
                UpsertSalesTargetBody(
                    storeId = UUID.randomUUID().toString(),
                    targetMonth = "2026-04-01",
                    targetAmount = 10000000,
                )

            // Act
            val response = resource.upsert(body)

            // Assert
            assertEquals(200, response.status)
        }

        @Test
        fun `storeId省略で作成可能`() {
            // Arrange
            val target = buildSalesTarget()
            whenever(stub.upsertSalesTarget(any())).thenReturn(
                UpsertSalesTargetResponse.newBuilder().setSalesTarget(target).build(),
            )
            val body =
                UpsertSalesTargetBody(
                    targetMonth = "2026-04-01",
                    targetAmount = 10000000,
                )

            // Act
            val response = resource.upsert(body)

            // Assert
            assertEquals(200, response.status)
        }

        @Test
        fun `0円以下の目標額でIllegalArgumentException`() {
            // Arrange
            val body =
                UpsertSalesTargetBody(
                    storeId = UUID.randomUUID().toString(),
                    targetMonth = "2026-04-01",
                    targetAmount = 0,
                )

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.upsert(body)
            }
        }

        @Test
        fun `負の目標額でIllegalArgumentException`() {
            // Arrange
            val body =
                UpsertSalesTargetBody(
                    storeId = UUID.randomUUID().toString(),
                    targetMonth = "2026-04-01",
                    targetAmount = -100,
                )

            // Act & Assert
            assertThrows<IllegalArgumentException> {
                resource.upsert(body)
            }
        }

        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"
            val body =
                UpsertSalesTargetBody(
                    storeId = UUID.randomUUID().toString(),
                    targetMonth = "2026-04-01",
                    targetAmount = 10000000,
                )

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.upsert(body)
            }
        }
    }

    @Nested
    inner class DeleteSalesTarget {
        @Test
        fun `削除で204を返す`() {
            // Arrange
            whenever(stub.deleteSalesTarget(any())).thenReturn(
                DeleteSalesTargetResponse.getDefaultInstance(),
            )

            // Act
            val response = resource.delete(targetId)

            // Assert
            assertEquals(204, response.status)
        }

        @Test
        fun `STAFF権限で削除するとForbiddenException`() {
            // Arrange
            tenantContext.staffRole = "STAFF"

            // Act & Assert
            assertThrows<ForbiddenException> {
                resource.delete(targetId)
            }
        }
    }
}
