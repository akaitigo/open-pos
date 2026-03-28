package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.pos.v1.GetTaxReportResponse
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.TaxReportItem
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class TaxReportResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext = TenantContext().apply { staffRole = "OWNER" }
    private val resource = TaxReportResource().also { r ->
        ProductResourceTest.setField(r, "stub", stub)
        ProductResourceTest.setField(r, "grpc", grpc)
        ProductResourceTest.setField(r, "tenantContext", tenantContext)
    }
    private val storeId = UUID.randomUUID().toString()

    @BeforeEach fun setUp() { whenever(grpc.withTenant(stub)).thenReturn(stub) }

    @Nested inner class GetTaxReport {
        @Test fun `税率別レポートを正しく返す`() {
            whenever(stub.getTaxReport(any())).thenReturn(GetTaxReportResponse.newBuilder()
                .addItems(TaxReportItem.newBuilder().setTaxRateName("標準税率10%").setTaxRatePercentage("0.10").setIsReduced(false).setTaxableAmount(300000).setTaxAmount(30000).setTransactionCount(5).build())
                .addItems(TaxReportItem.newBuilder().setTaxRateName("軽減税率8%").setTaxRatePercentage("0.08").setIsReduced(true).setTaxableAmount(100000).setTaxAmount(8000).setTransactionCount(3).build()).build())
            @Suppress("UNCHECKED_CAST")
            val data = resource.getTaxReport(storeId, "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z")["data"] as List<Map<String, Any?>>
            assertEquals(2, data.size); assertEquals("標準税率10%", data[0]["taxRateName"]); assertEquals(300000L, data[0]["taxableAmount"])
        }
        @Test fun `取引がない場合は空リスト`() {
            whenever(stub.getTaxReport(any())).thenReturn(GetTaxReportResponse.newBuilder().build())
            @Suppress("UNCHECKED_CAST")
            val data = resource.getTaxReport(storeId, "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z")["data"] as List<Map<String, Any?>>
            assertTrue(data.isEmpty())
        }
        @Test fun `CASHIERはアクセス拒否`() {
            tenantContext.staffRole = "CASHIER"
            org.junit.jupiter.api.assertThrows<com.openpos.gateway.config.ForbiddenException> { resource.getTaxReport(storeId, "2026-03-01T00:00:00Z", "2026-03-31T23:59:59Z") }
            tenantContext.staffRole = "OWNER"
        }
    }
}
