package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.PurchaseOrderEntity
import com.openpos.inventory.entity.PurchaseOrderItemEntity
import com.openpos.inventory.repository.PurchaseOrderItemRepository
import com.openpos.inventory.repository.PurchaseOrderRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
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
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class PurchaseOrderServiceTest {
    @Inject
    lateinit var purchaseOrderService: PurchaseOrderService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var purchaseOrderRepository: PurchaseOrderRepository

    @InjectMock
    lateinit var itemRepository: PurchaseOrderItemRepository

    @InjectMock
    lateinit var stockService: StockService

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(entity)

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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(null)

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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(entity)
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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(entity)
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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(entity)
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
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(entity)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
            }
        }

        @Test
        fun `存在しない発注の更新は例外を投げる`() {
            // Arrange
            val orderId = UUID.randomUUID()
            whenever(purchaseOrderRepository.findById(orderId)).thenReturn(null)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
            }
        }
    }
}
