package com.openpos.gateway.resource

import com.openpos.gateway.config.ForbiddenException
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import io.grpc.Status
import io.grpc.StatusRuntimeException
import openpos.common.v1.PaginationResponse
import openpos.inventory.v1.CompleteStockTransferResponse
import openpos.inventory.v1.CreateStockTransferResponse
import openpos.inventory.v1.GetStockTransferResponse
import openpos.inventory.v1.InventoryServiceGrpc
import openpos.inventory.v1.ListStockTransfersResponse
import openpos.inventory.v1.StockTransfer
import openpos.inventory.v1.StockTransferItem
import openpos.inventory.v1.StockTransferStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class StockTransferResourceTest {
    private val stub: InventoryServiceGrpc.InventoryServiceBlockingStub = mock()
    private val grpc: GrpcClientHelper = mock()
    private val tenantContext = TenantContext()
    private val resource =
        StockTransferResource().also { r ->
            ProductResourceTest.setField(r, "stub", stub)
            ProductResourceTest.setField(r, "grpc", grpc)
            ProductResourceTest.setField(r, "tenantContext", tenantContext)
        }

    private val orgId = UUID.randomUUID().toString()
    private val fromStoreId = UUID.randomUUID().toString()
    private val toStoreId = UUID.randomUUID().toString()
    private val transferId = UUID.randomUUID().toString()
    private val productId = UUID.randomUUID().toString()

    private fun buildStockTransfer(
        id: String = transferId,
        status: StockTransferStatus = StockTransferStatus.STOCK_TRANSFER_STATUS_PENDING,
    ): StockTransfer =
        StockTransfer
            .newBuilder()
            .setId(id)
            .setOrganizationId(orgId)
            .setFromStoreId(fromStoreId)
            .setToStoreId(toStoreId)
            .addItems(
                StockTransferItem
                    .newBuilder()
                    .setProductId(productId)
                    .setQuantity(10)
                    .build(),
            ).setStatus(status)
            .setNote("テスト移動")
            .setCreatedAt("2026-01-01T00:00:00Z")
            .setUpdatedAt("2026-01-01T00:00:00Z")
            .build()

    @BeforeEach
    fun setUp() {
        whenever(grpc.withTenant(stub)).thenReturn(stub)
        tenantContext.organizationId = UUID.fromString(orgId)
        tenantContext.staffRole = null
    }

    @Nested
    inner class Create {
        @Test
        fun `在庫移動作成で201を返す`() {
            // Arrange
            val transfer = buildStockTransfer()
            whenever(stub.createStockTransfer(any())).thenReturn(
                CreateStockTransferResponse.newBuilder().setStockTransfer(transfer).build(),
            )
            val body =
                CreateStockTransferBody(
                    fromStoreId = fromStoreId,
                    toStoreId = toStoreId,
                    items = listOf(StockTransferItemBody(productId = productId, quantity = 10)),
                    note = "テスト移動",
                )

            // Act
            val response = resource.create(body)

            // Assert
            assertEquals(201, response.status)
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, Any?>
            assertEquals(transferId, entity["id"])
            assertEquals("PENDING", entity["status"])
        }

        @Test
        fun `STAFF権限で作成するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            val body =
                CreateStockTransferBody(
                    fromStoreId = fromStoreId,
                    toStoreId = toStoreId,
                    items = listOf(StockTransferItemBody(productId = productId, quantity = 10)),
                )
            assertThrows<ForbiddenException> { resource.create(body) }
        }
    }

    @Nested
    inner class Get {
        @Test
        fun `在庫移動取得でMapを返す`() {
            // Arrange
            val transfer = buildStockTransfer()
            whenever(stub.getStockTransfer(any())).thenReturn(
                GetStockTransferResponse.newBuilder().setStockTransfer(transfer).build(),
            )

            // Act
            val result = resource.get(transferId)

            // Assert
            assertEquals(transferId, result["id"])
            assertEquals(fromStoreId, result["fromStoreId"])
            assertEquals(toStoreId, result["toStoreId"])
            assertEquals("PENDING", result["status"])
        }

        @Test
        fun `存在しない在庫移動でNOT_FOUND例外`() {
            whenever(stub.getStockTransfer(any())).thenThrow(
                StatusRuntimeException(Status.NOT_FOUND.withDescription("StockTransfer not found")),
            )

            assertThrows<StatusRuntimeException> {
                resource.get("nonexistent-id")
            }
        }

        @Test
        fun `STAFF権限で取得するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            assertThrows<ForbiddenException> { resource.get(transferId) }
        }
    }

    @Nested
    inner class ListTransfers {
        @Test
        fun `在庫移動一覧でページネーション付きMapを返す`() {
            // Arrange
            val transfer = buildStockTransfer()
            whenever(stub.listStockTransfers(any())).thenReturn(
                ListStockTransfersResponse
                    .newBuilder()
                    .addStockTransfers(transfer)
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
            val result = resource.list(storeId = null, status = null, page = 1, pageSize = 20)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(1, data.size)
        }

        @Test
        fun `storeIdフィルタを指定して一覧取得`() {
            // Arrange
            whenever(stub.listStockTransfers(any())).thenReturn(
                ListStockTransfersResponse
                    .newBuilder()
                    .setPagination(
                        PaginationResponse
                            .newBuilder()
                            .setPage(1)
                            .setPageSize(20)
                            .setTotalCount(0)
                            .setTotalPages(0)
                            .build(),
                    ).build(),
            )

            // Act
            val result = resource.list(storeId = fromStoreId, status = "PENDING", page = 1, pageSize = 20)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val data = result["data"] as List<*>
            assertEquals(0, data.size)
        }

        @Test
        fun `STAFF権限で一覧取得するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            assertThrows<ForbiddenException> { resource.list(storeId = null, status = null, page = 1, pageSize = 20) }
        }
    }

    @Nested
    inner class CompleteTransfer {
        @Test
        fun `在庫移動完了でCOMPLETEDステータスを返す`() {
            // Arrange
            val completedTransfer =
                buildStockTransfer(
                    status = StockTransferStatus.STOCK_TRANSFER_STATUS_COMPLETED,
                )
            whenever(stub.completeStockTransfer(any())).thenReturn(
                CompleteStockTransferResponse.newBuilder().setStockTransfer(completedTransfer).build(),
            )

            // Act
            val result = resource.complete(transferId)

            // Assert
            assertEquals(transferId, result["id"])
            assertEquals("COMPLETED", result["status"])
        }

        @Test
        fun `STAFF権限で完了するとForbiddenException`() {
            tenantContext.staffRole = "STAFF"
            assertThrows<ForbiddenException> { resource.complete(transferId) }
        }
    }
}
