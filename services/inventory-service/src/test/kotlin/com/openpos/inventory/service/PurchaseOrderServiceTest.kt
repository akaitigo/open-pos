package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.PurchaseOrderEntity
import com.openpos.inventory.entity.PurchaseOrderItemEntity
import com.openpos.inventory.repository.PurchaseOrderItemRepository
import com.openpos.inventory.repository.PurchaseOrderRepository
import io.grpc.StatusRuntimeException
import io.quarkus.panache.common.Page
import jakarta.persistence.OptimisticLockException
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.mockito.kotlin.mock
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class PurchaseOrderServiceTest {
    private lateinit var purchaseOrderService: PurchaseOrderService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private lateinit var purchaseOrderRepository: PurchaseOrderRepository

    private lateinit var itemRepository: PurchaseOrderItemRepository

    private lateinit var stockService: StockService

    private lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        purchaseOrderRepository = mock()
        itemRepository = mock()
        stockService = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        purchaseOrderService = PurchaseOrderService()
        purchaseOrderService.purchaseOrderRepository = purchaseOrderRepository
        purchaseOrderService.itemRepository = itemRepository
        purchaseOrderService.stockService = stockService
        purchaseOrderService.tenantFilterService = tenantFilterService
        purchaseOrderService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class Create {
        @Test
        fun `発注を正常に作成する`() {
            // Arrange
            val productId1 = UUID.randomUUID()
            val productId2 = UUID.randomUUID()
            doAnswer { invocation ->
                val entity = invocation.getArgument<PurchaseOrderEntity>(0)
                try {
                    entity.id
                } catch (_: UninitializedPropertyAccessException) {
                    entity.id = UUID.randomUUID()
                }
                null
            }.whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
            doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())

            val items =
                listOf(
                    PurchaseOrderItemInput(productId = productId1, orderedQuantity = 10, unitCost = 5000L),
                    PurchaseOrderItemInput(productId = productId2, orderedQuantity = 20, unitCost = 3000L),
                )

            // Act
            val result =
                purchaseOrderService.create(
                    storeId = storeId,
                    supplierName = "テスト仕入先",
                    note = "テスト発注",
                    items = items,
                )

            // Assert
            assertEquals(storeId, result.storeId)
            assertEquals("テスト仕入先", result.supplierName)
            assertEquals("テスト発注", result.note)
            assertEquals("DRAFT", result.status)
            assertEquals(orgId, result.organizationId)
            verify(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        }

        @Test
        fun `空の明細リストでは例外を投げる`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                purchaseOrderService.create(
                    storeId = storeId,
                    supplierName = "テスト仕入先",
                    note = null,
                    items = emptyList(),
                )
            }
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `IDで発注を取得する`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "DRAFT"
                }
            val mockQuery1 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery1)

            // Act
            val result = purchaseOrderService.findById(orderId)

            // Assert
            assertNotNull(result)
            assertEquals(orderId, result!!.id)
            assertEquals("テスト仕入先", result.supplierName)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery2)

            // Act
            val result = purchaseOrderService.findById(orderId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    @Nested
    inner class ListTest {
        @Test
        fun `店舗IDで発注一覧を取得する`() {
            // Arrange
            val order1 =
                PurchaseOrderEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "仕入先A"
                    this.status = "DRAFT"
                }
            whenever(purchaseOrderRepository.listByStoreId(eq(storeId), eq(null), any<Page>()))
                .thenReturn(listOf(order1))
            whenever(purchaseOrderRepository.countByStoreId(storeId, null)).thenReturn(1L)

            // Act
            val (orders, total) = purchaseOrderService.list(storeId, status = null, page = 0, pageSize = 20)

            // Assert
            assertEquals(1, orders.size)
            assertEquals(1L, total)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `ステータスでフィルタして発注一覧を取得する`() {
            // Arrange
            whenever(purchaseOrderRepository.listByStoreId(eq(storeId), eq("ORDERED"), any<Page>()))
                .thenReturn(emptyList())
            whenever(purchaseOrderRepository.countByStoreId(storeId, "ORDERED")).thenReturn(0L)

            // Act
            val (orders, total) = purchaseOrderService.list(storeId, status = "ORDERED", page = 0, pageSize = 20)

            // Assert
            assertEquals(0, orders.size)
            assertEquals(0L, total)
        }
    }

    @Nested
    inner class UpdateStatus {
        @Test
        fun `DRAFTからORDEREDに更新する`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "DRAFT"
                }
            val mockQuery3 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery3)
            doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

            // Act
            val result = purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())

            // Assert
            assertEquals("ORDERED", result.status)
            assertNotNull(result.orderedAt)
            verify(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        }

        @Test
        fun `DRAFTからCANCELLEDに更新する`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "DRAFT"
                }
            val mockQuery4 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery4)
            doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

            // Act
            val result = purchaseOrderService.updateStatus(orderId, "CANCELLED", emptyList())

            // Assert
            assertEquals("CANCELLED", result.status)
        }

        @Test
        fun `ORDEREDからRECEIVEDに更新すると在庫が反映される`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "ORDERED"
                }
            val item =
                PurchaseOrderItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.purchaseOrderId = orderId
                    this.productId = productId
                    this.orderedQuantity = 10
                    this.unitCost = 5000L
                }
            val mockQuery5 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery5)
            whenever(itemRepository.findByPurchaseOrderId(orderId)).thenReturn(listOf(item))
            doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
            doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())
            whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(
                com.openpos.inventory.entity.StockEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.productId = productId
                    this.quantity = 10
                },
            )

            // Act
            val result =
                purchaseOrderService.updateStatus(
                    orderId,
                    "RECEIVED",
                    listOf(ReceivedItemInput(productId = productId, receivedQuantity = 8)),
                )

            // Assert
            assertEquals("RECEIVED", result.status)
            assertNotNull(result.receivedAt)
            assertEquals(8, item.receivedQuantity)
            verify(stockService).adjustStock(
                storeId = eq(storeId),
                productId = eq(productId),
                quantityChange = eq(8),
                movementType = eq("RECEIPT"),
                referenceId = eq(orderId.toString()),
                note = any(),
            )
        }

        @Test
        fun `不正なステータス遷移は例外を投げる`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "RECEIVED"
                }
            val mockQuery6 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery6.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery6)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
            }
        }

        @Test
        fun `存在しない発注の更新は例外を投げる`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val mockQuery7 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery7.firstResult()).thenReturn(null)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery7)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
            }
        }

        @Test
        fun `楽観的ロック競合時はStatusRuntimeExceptionを投げる`() {
            // Arrange
            val orderId = UUID.randomUUID()
            val entity =
                PurchaseOrderEntity().apply {
                    this.id = orderId
                    this.organizationId = orgId
                    this.storeId = this@PurchaseOrderServiceTest.storeId
                    this.supplierName = "テスト仕入先"
                    this.status = "DRAFT"
                }
            val mockQuery8 = mock<PanacheQuery<PurchaseOrderEntity>>()
            whenever(mockQuery8.firstResult()).thenReturn(entity)
            whenever(purchaseOrderRepository.find(eq("id = ?1"), eq(orderId))).thenReturn(mockQuery8)
            doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
            doThrow(OptimisticLockException("concurrent modification"))
                .whenever(purchaseOrderRepository)
                .flush()

            // Act & Assert
            val exception =
                assertThrows(StatusRuntimeException::class.java) {
                    purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
                }
            assertEquals(io.grpc.Status.Code.ABORTED, exception.status.code)
        }
    }
}
