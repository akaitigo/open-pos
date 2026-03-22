package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.SessionTokenService
import openpos.common.v1.PaginationResponse
import openpos.store.v1.AuthenticateByPinResponse
import openpos.store.v1.CreateStaffResponse
import openpos.store.v1.GetStaffResponse
import openpos.store.v1.ListStaffResponse
import openpos.store.v1.Staff
import openpos.store.v1.StaffRole
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateStaffResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class StaffResourceTest {
    private val stub: StoreServiceGrpc.StoreServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val sessionTokenService: SessionTokenService = mock()
    private val tenantContext =
        com.openpos.gateway.config
            .TenantContext()
            .apply { staffRole = "OWNER" }
    private val resource =
        StaffResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "sessionTokenService", sessionTokenService)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()
    private val staffId = UUID.randomUUID().toString()

    private fun buildStaff(): Staff =
        Staff
            .newBuilder()
            .setId(staffId)
            .setOrganizationId(orgId)
            .setStoreId(storeId)
            .setName("田中太郎")
            .setEmail("tanaka@example.com")
            .setRole(StaffRole.STAFF_ROLE_CASHIER)
            .setIsActive(true)
            .setFailedPinAttempts(0)
            .setIsLocked(false)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
        whenever(sessionTokenService.generateToken(any(), any(), any(), any())).thenReturn("test-session-token")
    }

    @Nested
    inner class Create {
        @Test
        fun `スタッフ作成で201を返す`() {
            // Arrange
            whenever(stub.createStaff(any())).thenReturn(
                CreateStaffResponse.newBuilder().setStaff(buildStaff()).build(),
            )
            val body = CreateStaffBody(storeId = storeId, name = "田中太郎", pin = "1234")

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `スタッフ取得でMapを返す`() {
            // Arrange
            whenever(stub.getStaff(any())).thenReturn(
                GetStaffResponse.newBuilder().setStaff(buildStaff()).build(),
            )

            // Act
            val result = resource.get(staffId)

            // Assert
            assertEquals("田中太郎", result["name"])
            assertEquals("CASHIER", result["role"])
        }
    }

    @Nested
    inner class List {
        @Test
        fun `スタッフ一覧でページネーション付きMapを返す`() {
            // Arrange
            whenever(stub.listStaff(any())).thenReturn(
                ListStaffResponse
                    .newBuilder()
                    .addStaff(buildStaff())
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(1)
                            .setTotalPages(1)
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.list(storeId = storeId, page = 1, pageSize = 20)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as kotlin.collections.List<*>
            assertEquals(1, data.size)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `スタッフ更新でMapを返す`() {
            // Arrange
            whenever(stub.updateStaff(any())).thenReturn(
                UpdateStaffResponse.newBuilder().setStaff(buildStaff()).build(),
            )

            // Act
            val result = resource.update(staffId, UpdateStaffBody(name = "田中次郎"))

            // Assert
            assertEquals("田中太郎", result["name"])
        }
    }

    @Nested
    inner class Authenticate {
        @Test
        fun `PIN認証成功`() {
            // Arrange
            whenever(stub.authenticateByPin(any())).thenReturn(
                AuthenticateByPinResponse
                    .newBuilder()
                    .setSuccess(true)
                    .setStaff(buildStaff())
                    .build(),
            )

            // Act
            val result = resource.authenticate(staffId, AuthenticateBody(storeId = storeId, pin = "1234"))

            // Assert
            assertEquals(true, result["success"])
        }

        @Test
        fun `PIN認証失敗`() {
            // Arrange
            whenever(stub.authenticateByPin(any())).thenReturn(
                AuthenticateByPinResponse
                    .newBuilder()
                    .setSuccess(false)
                    .setReason("INVALID_PIN")
                    .build(),
            )

            // Act
            val result = resource.authenticate(staffId, AuthenticateBody(storeId = storeId, pin = "9999"))

            // Assert
            assertEquals(false, result["success"])
            assertEquals("INVALID_PIN", result["reason"])
        }
    }
}
