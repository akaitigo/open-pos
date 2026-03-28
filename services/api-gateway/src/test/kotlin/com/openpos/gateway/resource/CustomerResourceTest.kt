package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.common.v1.PaginationResponse
import openpos.store.v1.CreateCustomerResponse
import openpos.store.v1.Customer
import openpos.store.v1.CustomerTier
import openpos.store.v1.EarnPointsResponse
import openpos.store.v1.GetCustomerResponse
import openpos.store.v1.ListCustomersResponse
import openpos.store.v1.RedeemPointsResponse
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateCustomerResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class CustomerResourceTest {
    private val stub: StoreServiceGrpc.StoreServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext = TenantContext()
    private val resource =
        CustomerResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val customerId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        tenantContext.staffRole = null
        tenantContext.organizationId = UUID.fromString(orgId)
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    private fun buildCustomer(
        id: String = customerId,
        points: Long = 0,
    ): Customer =
        Customer
            .newBuilder()
            .setId(id)
            .setOrganizationId(orgId)
            .setName("テスト顧客")
            .setEmail("test@example.com")
            .setPhone("090-1234-5678")
            .setPoints(points)
            .setTier(CustomerTier.CUSTOMER_TIER_REGULAR)
            .setIsActive(true)
            .setNotes("")
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @Nested
    inner class CreateCustomer {
        @Test
        fun `顧客作成で201を返す`() {
            val customer = buildCustomer()
            whenever(stub.createCustomer(any())).thenReturn(
                CreateCustomerResponse.newBuilder().setCustomer(customer).build(),
            )
            val body = CreateCustomerBody(name = "テスト顧客")
            val response = resource.create(body)
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class GetCustomer {
        @Test
        fun `顧客取得でIDと名前を返す`() {
            val customer = buildCustomer()
            whenever(stub.getCustomer(any())).thenReturn(
                GetCustomerResponse.newBuilder().setCustomer(customer).build(),
            )
            val result = resource.get(customerId)
            assertEquals(customerId, result["id"])
            assertEquals("テスト顧客", result["name"])
        }
    }

    @Nested
    inner class ListCustomers {
        @Test
        fun `ページネーション付きで一覧を返す`() {
            val customer = buildCustomer()
            whenever(stub.listCustomers(any())).thenReturn(
                ListCustomersResponse
                    .newBuilder()
                    .addCustomers(customer)
                    .setPagination(
                        PaginationResponse.newBuilder().setPage(1).setPageSize(20).setTotalCount(1).setTotalPages(1).build(),
                    ).build(),
            )
            val result = resource.list(page = 1, pageSize = 20, search = null)
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(1, data.size)
        }
    }

    @Nested
    inner class UpdateCustomer {
        @Test
        fun `顧客更新で更新後の情報を返す`() {
            val customer = buildCustomer()
            whenever(stub.updateCustomer(any())).thenReturn(
                UpdateCustomerResponse.newBuilder().setCustomer(customer).build(),
            )
            val body = UpdateCustomerBody(name = "新しい名前")
            val result = resource.update(customerId, body)
            assertEquals(customerId, result["id"])
        }
    }

    @Nested
    inner class EarnPoints {
        @Test
        fun `ポイント付与でearnedPointsと顧客情報を返す`() {
            val customer = buildCustomer(points = 10)
            whenever(stub.earnPoints(any())).thenReturn(
                EarnPointsResponse.newBuilder().setEarnedPoints(10).setCustomer(customer).build(),
            )
            val body = EarnPointsBody(transactionTotal = 100000)
            val result = resource.earnPoints(customerId, body)
            assertEquals(10L, result["earnedPoints"])
        }
    }

    @Nested
    inner class RedeemPoints {
        @Test
        fun `ポイント利用で成功フラグを返す`() {
            val customer = buildCustomer(points = 70)
            whenever(stub.redeemPoints(any())).thenReturn(
                RedeemPointsResponse.newBuilder().setSuccess(true).setCustomer(customer).build(),
            )
            val body = RedeemPointsBody(points = 30)
            val result = resource.redeemPoints(customerId, body)
            assertEquals(true, result["success"])
        }

        @Test
        fun `ポイント不足で失敗を返す`() {
            val customer = buildCustomer(points = 5)
            whenever(stub.redeemPoints(any())).thenReturn(
                RedeemPointsResponse.newBuilder().setSuccess(false).setCustomer(customer).build(),
            )
            val body = RedeemPointsBody(points = 30)
            val result = resource.redeemPoints(customerId, body)
            assertEquals(false, result["success"])
        }
    }
}
