package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import openpos.pos.v1.CreateSettlementResponse
import openpos.pos.v1.GetSettlementResponse
import openpos.pos.v1.PosServiceGrpc
import openpos.pos.v1.Settlement
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class SettlementResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        SettlementResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val settlementId = UUID.randomUUID().toString()
    private val orgId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()
    private val terminalId = UUID.randomUUID().toString()
    private val staffId = UUID.randomUUID().toString()

    private val sampleSettlement =
        Settlement
            .newBuilder()
            .setId(settlementId)
            .setOrganizationId(orgId)
            .setStoreId(storeId)
            .setTerminalId(terminalId)
            .setStaffId(staffId)
            .setCashExpected(50000)
            .setCashActual(48000)
            .setDifference(-2000)
            .setSettledAt("2026-03-15T18:00:00Z")
            .setCreatedAt("2026-03-15T18:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class CreateSettlement {
        @Test
        fun `精算作成でCREATEDを返す`() {
            // Arrange
            whenever(stub.createSettlement(any())).thenReturn(
                CreateSettlementResponse.newBuilder().setSettlement(sampleSettlement).build(),
            )

            // Act
            val response = resource.create(CreateSettlementBody(storeId, terminalId, staffId, 48000))

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals(settlementId, entity["id"])
            assertEquals(50000L, entity["cashExpected"])
            assertEquals(48000L, entity["cashActual"])
            assertEquals(-2000L, entity["difference"])
        }
    }

    @Nested
    inner class GetSettlement {
        @Test
        fun `精算情報を返す`() {
            // Arrange
            whenever(stub.getSettlement(any())).thenReturn(
                GetSettlementResponse.newBuilder().setSettlement(sampleSettlement).build(),
            )

            // Act
            val result = resource.get(settlementId)

            // Assert
            assertEquals(settlementId, result["id"])
            assertEquals(storeId, result["storeId"])
            assertEquals(50000L, result["cashExpected"])
            assertEquals(-2000L, result["difference"])
        }
    }
}
