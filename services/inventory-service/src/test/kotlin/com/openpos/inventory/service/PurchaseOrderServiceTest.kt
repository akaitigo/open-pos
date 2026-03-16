package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.PurchaseOrderEntity
import com.openpos.inventory.entity.PurchaseOrderItemEntity
import com.openpos.inventory.entity.StockEntity
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
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class PurchaseOrderServiceTest {
    @Inject
    lateinit var purchaseOrderService: PurchaseOrderService

    @InjectMock
    lateinit var purchaseOrderRepository: PurchaseOrderRepository

    @InjectMock
    lateinit var itemRepository: PurchaseOrderItemRepository

    @InjectMock
    lateinit var stockService: StockService

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId1 = UUID.randomUUID()
    private val productId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Test
    fun `create persists order and items`() {
        // Arrange
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())

        val items =
            listOf(
                PurchaseOrderItemInput(productId = productId1, orderedQuantity = 10, unitCost = 50000L),
                PurchaseOrderItemInput(productId = productId2, orderedQuantity = 5, unitCost = 30000L),
            )

        // Act
        val result =
            purchaseOrderService.create(
                storeId = storeId,
                supplierName = "Supplier A",
                note = "Test order",
                items = items,
            )

        // Assert
        assertEquals(storeId, result.storeId)
        assertEquals("Supplier A", result.supplierName)
        assertEquals("Test order", result.note)
        assertEquals("DRAFT", result.status)
        assertEquals(orgId, result.organizationId)
        verify(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
    }

    @Test
    fun `create sets status to DRAFT`() {
        // Arrange
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())

        val items =
            listOf(
                PurchaseOrderItemInput(productId = productId1, orderedQuantity = 1, unitCost = 10000L),
            )

        // Act
        val result =
            purchaseOrderService.create(
                storeId = storeId,
                supplierName = "Supplier B",
                note = null,
                items = items,
            )

        // Assert
        assertEquals("DRAFT", result.status)
        assertNull(result.orderedAt)
        assertNull(result.receivedAt)
    }

    @Test
    fun `create throws when items list is empty`() {
        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.create(
                storeId = storeId,
                supplierName = "Supplier",
                note = null,
                items = emptyList(),
            )
        }
    }

    @Test
    fun `create throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        val items =
            listOf(
                PurchaseOrderItemInput(productId = productId1, orderedQuantity = 5, unitCost = 10000L),
            )

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.create(
                storeId = storeId,
                supplierName = "Supplier",
                note = null,
                items = items,
            )
        }
    }

    @Test
    fun `create persists correct number of items`() {
        // Arrange
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())

        val items =
            listOf(
                PurchaseOrderItemInput(productId = productId1, orderedQuantity = 10, unitCost = 50000L),
                PurchaseOrderItemInput(productId = productId2, orderedQuantity = 5, unitCost = 30000L),
            )

        // Act
        purchaseOrderService.create(
            storeId = storeId,
            supplierName = "Supplier",
            note = null,
            items = items,
        )

        // Assert - verify persist called for each item
        verify(itemRepository).persist(any<PurchaseOrderItemEntity>())
    }

    // === findById ===

    @Test
    fun `findById returns order when found`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "DRAFT")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)

        // Act
        val result = purchaseOrderService.findById(orderId)

        // Assert
        assertNotNull(result)
        assertEquals(orderId, result?.id)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `findById returns null when not found`() {
        // Arrange
        val orderId = UUID.randomUUID()
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(null)

        // Act
        val result = purchaseOrderService.findById(orderId)

        // Assert
        assertNull(result)
    }

    // === getItems ===

    @Test
    fun `getItems returns items for purchase order`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val items =
            listOf(
                createItemEntity(orderId, productId1, orderedQuantity = 10),
                createItemEntity(orderId, productId2, orderedQuantity = 5),
            )
        whenever(itemRepository.findByPurchaseOrderId(orderId)).thenReturn(items)

        // Act
        val result = purchaseOrderService.getItems(orderId)

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `getItems returns empty list when no items exist`() {
        // Arrange
        val orderId = UUID.randomUUID()
        whenever(itemRepository.findByPurchaseOrderId(orderId)).thenReturn(emptyList())

        // Act
        val result = purchaseOrderService.getItems(orderId)

        // Assert
        assertEquals(0, result.size)
    }

    // === list ===

    @Test
    fun `list returns orders with pagination`() {
        // Arrange
        val orders =
            listOf(
                createOrderEntity(status = "DRAFT"),
                createOrderEntity(status = "ORDERED"),
            )
        whenever(purchaseOrderRepository.listByStoreId(eq(storeId), eq(null), any<Page>())).thenReturn(orders)
        whenever(purchaseOrderRepository.countByStoreId(storeId, null)).thenReturn(2L)

        // Act
        val (result, totalCount) = purchaseOrderService.list(storeId, status = null, page = 0, pageSize = 20)

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `list with status filter returns matching orders`() {
        // Arrange
        val orders = listOf(createOrderEntity(status = "ORDERED"))
        whenever(purchaseOrderRepository.listByStoreId(eq(storeId), eq("ORDERED"), any<Page>())).thenReturn(orders)
        whenever(purchaseOrderRepository.countByStoreId(storeId, "ORDERED")).thenReturn(1L)

        // Act
        val (result, totalCount) = purchaseOrderService.list(storeId, status = "ORDERED", page = 0, pageSize = 20)

        // Assert
        assertEquals(1, result.size)
        assertEquals(1L, totalCount)
    }

    @Test
    fun `list returns empty list when no orders exist`() {
        // Arrange
        whenever(purchaseOrderRepository.listByStoreId(eq(storeId), eq(null), any<Page>())).thenReturn(emptyList())
        whenever(purchaseOrderRepository.countByStoreId(storeId, null)).thenReturn(0L)

        // Act
        val (result, totalCount) = purchaseOrderService.list(storeId, status = null, page = 0, pageSize = 20)

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    // === updateStatus: DRAFT -> ORDERED ===

    @Test
    fun `updateStatus transitions from DRAFT to ORDERED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "DRAFT")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

        // Act
        val result = purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())

        // Assert
        assertEquals("ORDERED", result.status)
        assertNotNull(result.orderedAt)
        verify(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
    }

    // === updateStatus: DRAFT -> CANCELLED ===

    @Test
    fun `updateStatus transitions from DRAFT to CANCELLED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "DRAFT")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

        // Act
        val result = purchaseOrderService.updateStatus(orderId, "CANCELLED", emptyList())

        // Assert
        assertEquals("CANCELLED", result.status)
    }

    // === updateStatus: ORDERED -> RECEIVED ===

    @Test
    fun `updateStatus transitions from ORDERED to RECEIVED and adjusts stock`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "ORDERED")
        val items =
            listOf(
                createItemEntity(orderId, productId1, orderedQuantity = 10),
            )
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        whenever(itemRepository.findByPurchaseOrderId(orderId)).thenReturn(items)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())
        whenever(
            stockService.adjustStock(
                storeId = eq(storeId),
                productId = eq(productId1),
                quantityChange = eq(10),
                movementType = eq("RECEIPT"),
                referenceId = any(),
                note = any(),
            ),
        ).thenReturn(createStockEntity(10))

        // Act
        val result = purchaseOrderService.updateStatus(orderId, "RECEIVED", emptyList())

        // Assert
        assertEquals("RECEIVED", result.status)
        assertNotNull(result.receivedAt)
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(10),
            movementType = eq("RECEIPT"),
            referenceId = any(),
            note = any(),
        )
    }

    @Test
    fun `updateStatus RECEIVED with receivedItems uses provided quantities`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "ORDERED")
        val items =
            listOf(
                createItemEntity(orderId, productId1, orderedQuantity = 10),
            )
        val receivedItems =
            listOf(
                ReceivedItemInput(productId = productId1, receivedQuantity = 8),
            )
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        whenever(itemRepository.findByPurchaseOrderId(orderId)).thenReturn(items)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())
        doNothing().whenever(itemRepository).persist(any<PurchaseOrderItemEntity>())
        whenever(
            stockService.adjustStock(
                storeId = eq(storeId),
                productId = eq(productId1),
                quantityChange = eq(8),
                movementType = eq("RECEIPT"),
                referenceId = any(),
                note = any(),
            ),
        ).thenReturn(createStockEntity(8))

        // Act
        val result = purchaseOrderService.updateStatus(orderId, "RECEIVED", receivedItems)

        // Assert
        assertEquals("RECEIVED", result.status)
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(8),
            movementType = eq("RECEIPT"),
            referenceId = any(),
            note = any(),
        )
    }

    // === updateStatus: ORDERED -> CANCELLED ===

    @Test
    fun `updateStatus transitions from ORDERED to CANCELLED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "ORDERED")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

        // Act
        val result = purchaseOrderService.updateStatus(orderId, "CANCELLED", emptyList())

        // Assert
        assertEquals("CANCELLED", result.status)
    }

    // === updateStatus: invalid transitions ===

    @Test
    fun `updateStatus throws for invalid transition DRAFT to RECEIVED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "DRAFT")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.updateStatus(orderId, "RECEIVED", emptyList())
        }
    }

    @Test
    fun `updateStatus throws for invalid transition RECEIVED to ORDERED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "RECEIVED")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
        }
    }

    @Test
    fun `updateStatus throws for invalid transition CANCELLED to ORDERED`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "CANCELLED")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
        }
    }

    @Test
    fun `updateStatus throws when order not found`() {
        // Arrange
        val orderId = UUID.randomUUID()
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(null)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            purchaseOrderService.updateStatus(orderId, "ORDERED", emptyList())
        }
    }

    @Test
    fun `updateStatus CANCELLED does not adjust stock`() {
        // Arrange
        val orderId = UUID.randomUUID()
        val order = createOrderEntity(orderId, status = "ORDERED")
        whenever(purchaseOrderRepository.findById(orderId)).thenReturn(order)
        doNothing().whenever(purchaseOrderRepository).persist(any<PurchaseOrderEntity>())

        // Act
        purchaseOrderService.updateStatus(orderId, "CANCELLED", emptyList())

        // Assert
        verify(stockService, never()).adjustStock(
            storeId = any(),
            productId = any(),
            quantityChange = any(),
            movementType = any(),
            referenceId = any(),
            note = any(),
        )
    }

    // === Helpers ===

    private fun createOrderEntity(
        id: UUID = UUID.randomUUID(),
        status: String = "DRAFT",
    ): PurchaseOrderEntity =
        PurchaseOrderEntity().apply {
            this.id = id
            this.organizationId = orgId
            this.storeId = this@PurchaseOrderServiceTest.storeId
            this.supplierName = "Test Supplier"
            this.note = null
            this.status = status
        }

    private fun createItemEntity(
        purchaseOrderId: UUID,
        productId: UUID,
        orderedQuantity: Int = 10,
    ): PurchaseOrderItemEntity =
        PurchaseOrderItemEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.purchaseOrderId = purchaseOrderId
            this.productId = productId
            this.orderedQuantity = orderedQuantity
            this.receivedQuantity = 0
            this.unitCost = 10000L
        }

    private fun createStockEntity(quantity: Int): StockEntity =
        StockEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@PurchaseOrderServiceTest.storeId
            this.productId = productId1
            this.quantity = quantity
            this.lowStockThreshold = 10
        }
}
