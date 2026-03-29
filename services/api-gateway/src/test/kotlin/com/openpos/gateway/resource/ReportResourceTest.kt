package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.pos.v1.GetStaffSalesReportResponse
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.StaffSalesItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class ReportResourceTest {
    private val posStub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext =
        TenantContext().apply { staffRole = "OWNER" }
    private val resource =
        ReportResource().also { r ->
            ProductResourceTest.setField(r, "posStub", posStub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val storeId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(posStub)).thenReturn(posStub)
    }

    @Nested
    inner class GetStaffSalesReport {
        @Test
        fun `returns staff sales report`() {
            // Arrange
            val staffId = UUID.randomUUID().toString()
            val item =
                StaffSalesItem
                    .newBuilder()
                    .setStaffId(staffId)
                    .setStaffName("Tanaka")
                    .setTotalAmount(1000000)
                    .setTransactionCount(50)
                    .setAverageTransaction(20000)
                    .build()
            whenever(posStub.getStaffSalesReport(any())).thenReturn(
                GetStaffSalesReportResponse.newBuilder().addItems(item).build(),
            )

            // Act
            val result = resource.getStaffSalesReport(storeId, "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z")

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<Map<String, Any?>>
            assertEquals(1, data.size)
            assertEquals(staffId, data[0]["staffId"])
            assertEquals("Tanaka", data[0]["staffName"])
            assertEquals(1000000L, data[0]["totalAmount"])
            assertEquals(50, data[0]["transactionCount"])
            assertEquals(20000L, data[0]["averageTransaction"])
        }

        @Test
        fun `returns empty report`() {
            // Arrange
            whenever(posStub.getStaffSalesReport(any())).thenReturn(
                GetStaffSalesReportResponse.getDefaultInstance(),
            )

            // Act
            val result = resource.getStaffSalesReport(storeId, "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z")

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<Map<String, Any?>>
            assertEquals(0, data.size)
        }
    }
}
