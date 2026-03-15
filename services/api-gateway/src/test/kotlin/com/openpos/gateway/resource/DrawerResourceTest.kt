package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import openpos.pos.v1.CloseDrawerResponse
import openpos.pos.v1.Drawer
import openpos.pos.v1.GetDrawerStatusResponse
import openpos.pos.v1.OpenDrawerResponse
import openpos.pos.v1.PosServiceGrpc
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class DrawerResourceTest {
    private val stub: PosServiceGrpc.PosServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        DrawerResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val drawerId = UUID.randomUUID().toString()
    private val orgId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()
    private val terminalId = UUID.randomUUID().toString()

    private val sampleDrawer =
        Drawer
            .newBuilder()
            .setId(drawerId)
            .setOrganizationId(orgId)
            .setStoreId(storeId)
            .setTerminalId(terminalId)
            .setOpeningAmount(30000)
            .setCurrentAmount(30000)
            .setIsOpen(true)
            .setOpenedAt("2026-03-15T09:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
    }

    @Nested
    inner class OpenDrawer {
        @Test
        fun `ドロワー開局でCREATEDを返す`() {
            // Arrange
            whenever(stub.openDrawer(any())).thenReturn(
                OpenDrawerResponse.newBuilder().setDrawer(sampleDrawer).build(),
            )

            // Act
            val response = resource.open(OpenDrawerBody(storeId, terminalId, 30000))

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals(drawerId, entity["id"])
            assertEquals(30000L, entity["openingAmount"])
            assertEquals(true, entity["isOpen"])
        }
    }

    @Nested
    inner class CloseDrawer {
        @Test
        fun `ドロワー閉局でドロワー情報を返す`() {
            // Arrange
            val closedDrawer =
                sampleDrawer
                    .toBuilder()
                    .setIsOpen(false)
                    .setClosedAt("2026-03-15T18:00:00Z")
                    .build()
            whenever(stub.closeDrawer(any())).thenReturn(
                CloseDrawerResponse.newBuilder().setDrawer(closedDrawer).build(),
            )

            // Act
            val result = resource.close(CloseDrawerBody(storeId, terminalId))

            // Assert
            assertEquals(false, result["isOpen"])
            assertEquals("2026-03-15T18:00:00Z", result["closedAt"])
        }
    }

    @Nested
    inner class GetDrawerStatus {
        @Test
        fun `ドロワー状態を返す`() {
            // Arrange
            whenever(stub.getDrawerStatus(any())).thenReturn(
                GetDrawerStatusResponse.newBuilder().setDrawer(sampleDrawer).build(),
            )

            // Act
            val result = resource.getStatus(storeId, terminalId)

            // Assert
            assertEquals(drawerId, result["id"])
            assertEquals(true, result["isOpen"])
            assertEquals(30000L, result["openingAmount"])
        }
    }
}
