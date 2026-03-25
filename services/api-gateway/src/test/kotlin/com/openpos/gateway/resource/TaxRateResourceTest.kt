package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import openpos.product.v1.CreateTaxRateResponse
import openpos.product.v1.ListTaxRatesResponse
import openpos.product.v1.ProductServiceGrpc
import openpos.product.v1.TaxRate
import openpos.product.v1.UpdateTaxRateResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class TaxRateResourceTest {
    private val stub: ProductServiceGrpc.ProductServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        TaxRateResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", TenantContext())
        }

    private val orgId = UUID.randomUUID().toString()

    private fun buildTaxRate(): TaxRate =
        TaxRate
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setOrganizationId(orgId)
            .setName("標準税率10%")
            .setRate("0.10")
            .setIsReduced(false)
            .setIsDefault(true)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class Create {
        @Test
        fun `税率作成で201を返す`() {
            // Arrange
            whenever(stub.createTaxRate(any())).thenReturn(
                CreateTaxRateResponse.newBuilder().setTaxRate(buildTaxRate()).build(),
            )

            // Act
            val response = resource.create(CreateTaxRateBody(name = "標準税率10%", rate = "0.10"))

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class List {
        @Test
        fun `税率一覧を返す`() {
            // Arrange
            whenever(stub.listTaxRates(any())).thenReturn(
                ListTaxRatesResponse.newBuilder().addTaxRates(buildTaxRate()).build(),
            )

            // Act
            val result = resource.list()

            // Assert
            assertEquals(1, result.size)
            assertEquals("標準税率10%", result[0]["name"])
            assertEquals("0.10", result[0]["rate"])
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `税率更新でMapを返す`() {
            // Arrange
            whenever(stub.updateTaxRate(any())).thenReturn(
                UpdateTaxRateResponse.newBuilder().setTaxRate(buildTaxRate()).build(),
            )

            // Act
            val result = resource.update("tax-id", UpdateTaxRateBody(name = "軽減税率8%", rate = "0.08"))

            // Assert
            assertEquals("標準税率10%", result["name"])
        }

        @Test
        fun `軽減税率フラグ付きで更新`() {
            // Arrange
            val reduced =
                buildTaxRate()
                    .toBuilder()
                    .setIsReduced(true)
                    .setName("軽減税率8%")
                    .setRate("0.08")
                    .build()
            whenever(stub.updateTaxRate(any())).thenReturn(
                UpdateTaxRateResponse.newBuilder().setTaxRate(reduced).build(),
            )

            // Act
            val result =
                resource.update(
                    "tax-id",
                    UpdateTaxRateBody(isReduced = true, isDefault = false),
                )

            // Assert
            assertEquals(true, result["isReduced"])
        }
    }
}
