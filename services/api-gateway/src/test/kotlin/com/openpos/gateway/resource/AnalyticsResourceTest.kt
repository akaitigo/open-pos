package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import openpos.analytics.v1.AnalyticsServiceGrpc
import openpos.analytics.v1.DailySales
import openpos.analytics.v1.GetDailySalesResponse
import openpos.analytics.v1.GetHourlySalesResponse
import openpos.analytics.v1.GetSalesSummaryResponse
import openpos.analytics.v1.HourlySales
import openpos.analytics.v1.SalesSummary
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class AnalyticsResourceTest {
    private val stub: AnalyticsServiceGrpc.AnalyticsServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        AnalyticsResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val storeId = UUID.randomUUID().toString()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class GetDailySales {
        @Test
        fun `日次売上一覧を返す`() {
            // Arrange
            val dailySales =
                DailySales
                    .newBuilder()
                    .setDate("2026-01-15")
                    .setStoreId(storeId)
                    .setGrossAmount(1000000)
                    .setNetAmount(909091)
                    .setTaxAmount(90909)
                    .setTransactionCount(50)
                    .setCashAmount(600000)
                    .setCardAmount(300000)
                    .setQrAmount(100000)
                    .build()
            whenever(stub.getDailySales(any())).thenReturn(
                GetDailySalesResponse.newBuilder().addDailySales(dailySales).build(),
            )

            // Act
            val result = resource.getDailySales(storeId, "2026-01-15", "2026-01-15")

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<Map<String, Any?>>
            assertEquals(1, data.size)
            assertEquals("2026-01-15", data[0]["date"])
            assertEquals(1000000L, data[0]["grossAmount"])
            assertEquals(50, data[0]["transactionCount"])
        }
    }

    @Nested
    inner class GetSalesSummary {
        @Test
        fun `売上サマリーを返す`() {
            // Arrange
            val summary =
                SalesSummary
                    .newBuilder()
                    .setTotalGross(5000000)
                    .setTotalNet(4545455)
                    .setTotalTax(454545)
                    .setTotalTransactions(200)
                    .setAverageTransaction(25000)
                    .build()
            whenever(stub.getSalesSummary(any())).thenReturn(
                GetSalesSummaryResponse.newBuilder().setSummary(summary).build(),
            )

            // Act
            val result = resource.getSalesSummary(storeId, "2026-01-01", "2026-01-31")

            // Assert
            assertEquals(5000000L, result["totalGross"])
            assertEquals(200, result["totalTransactions"])
            assertEquals(25000L, result["averageTransaction"])
        }
    }

    @Nested
    inner class GetHourlySales {
        @Test
        fun `時間帯別売上を返す`() {
            // Arrange
            val hourly =
                HourlySales
                    .newBuilder()
                    .setHour(12)
                    .setAmount(500000)
                    .setTransactionCount(20)
                    .build()
            whenever(stub.getHourlySales(any())).thenReturn(
                GetHourlySalesResponse.newBuilder().addHourlySales(hourly).build(),
            )

            // Act
            val result = resource.getHourlySales(storeId, "2026-01-15")

            // Assert
            assertEquals(1, result.size)
            assertEquals(12, result[0]["hour"])
            assertEquals(500000L, result[0]["amount"])
        }
    }
}
