package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import openpos.common.v1.PaginationResponse
import openpos.store.v1.CreateStoreResponse
import openpos.store.v1.GetStoreResponse
import openpos.store.v1.ListStoresResponse
import openpos.store.v1.ListTerminalsResponse
import openpos.store.v1.RegisterTerminalResponse
import openpos.store.v1.Store
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.Terminal
import openpos.store.v1.UpdateStoreResponse
import openpos.store.v1.UpdateTerminalSyncResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class StoreResourceTest {
    private val stub: StoreServiceGrpc.StoreServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val resource =
        StoreResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
        }

    private val orgId = UUID.randomUUID().toString()
    private val storeId = UUID.randomUUID().toString()

    private fun buildStore(): Store =
        Store
            .newBuilder()
            .setId(storeId)
            .setOrganizationId(orgId)
            .setName("本店")
            .setAddress("東京都渋谷区")
            .setPhone("03-1234-5678")
            .setTimezone("Asia/Tokyo")
            .setIsActive(true)
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    private fun buildTerminal(): Terminal =
        Terminal
            .newBuilder()
            .setId(UUID.randomUUID().toString())
            .setOrganizationId(orgId)
            .setStoreId(storeId)
            .setTerminalCode("POS-01")
            .setName("レジ1番")
            .setIsActive(true)
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
        fun `店舗作成で201を返す`() {
            // Arrange
            whenever(stub.createStore(any())).thenReturn(
                CreateStoreResponse.newBuilder().setStore(buildStore()).build(),
            )

            // Act
            val response = resource.create(CreateStoreBody(name = "本店"))

            // Assert
            assertEquals(201, response.status)
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `店舗取得でMapを返す`() {
            // Arrange
            whenever(stub.getStore(any())).thenReturn(
                GetStoreResponse.newBuilder().setStore(buildStore()).build(),
            )

            // Act
            val result = resource.get(storeId)

            // Assert
            assertEquals("本店", result["name"])
            assertEquals("Asia/Tokyo", result["timezone"])
        }
    }

    @Nested
    inner class List {
        @Test
        fun `店舗一覧でページネーション付きMapを返す`() {
            // Arrange
            whenever(stub.listStores(any())).thenReturn(
                ListStoresResponse
                    .newBuilder()
                    .addStores(buildStore())
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
            val result = resource.list(page = 1, pageSize = 20)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(1, data.size)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `店舗更新でMapを返す`() {
            // Arrange
            whenever(stub.updateStore(any())).thenReturn(
                UpdateStoreResponse.newBuilder().setStore(buildStore()).build(),
            )

            // Act
            val result = resource.update(storeId, UpdateStoreBody(name = "支店"))

            // Assert
            assertEquals("本店", result["name"])
        }
    }

    @Nested
    inner class Terminals {
        @Test
        fun `端末登録で201を返す`() {
            // Arrange
            whenever(stub.registerTerminal(any())).thenReturn(
                RegisterTerminalResponse.newBuilder().setTerminal(buildTerminal()).build(),
            )

            // Act
            val response = resource.registerTerminal(storeId, RegisterTerminalBody(terminalCode = "POS-01", name = "レジ1番"))

            // Assert
            assertEquals(201, response.status)
        }

        @Test
        fun `端末一覧を返す`() {
            // Arrange
            whenever(stub.listTerminals(any())).thenReturn(
                ListTerminalsResponse.newBuilder().addTerminals(buildTerminal()).build(),
            )

            // Act
            val result = resource.listTerminals(storeId)

            // Assert
            assertEquals(1, result.size)
            assertEquals("POS-01", result[0]["terminalCode"])
        }

        @Test
        fun `端末同期更新でMapを返す`() {
            // Arrange
            whenever(stub.updateTerminalSync(any())).thenReturn(
                UpdateTerminalSyncResponse.newBuilder().setTerminal(buildTerminal()).build(),
            )

            // Act
            val result = resource.updateTerminalSync(storeId, "terminal-id")

            // Assert
            assertEquals("POS-01", result["terminalCode"])
        }
    }
}
