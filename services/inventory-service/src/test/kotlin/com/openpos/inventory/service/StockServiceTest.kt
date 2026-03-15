package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StockMovementEntity
import com.openpos.inventory.event.StockLowEventPublisher
import com.openpos.inventory.repository.StockMovementRepository
import com.openpos.inventory.repository.StockRepository
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
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@QuarkusTest
class StockServiceTest {
    @Inject
    lateinit var stockService: StockService

    @InjectMock
    lateinit var stockRepository: StockRepository

    @InjectMock
    lateinit var movementRepository: StockMovementRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var stockLowEventPublisher: StockLowEventPublisher

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(stockLowEventPublisher).publish(any(), any(), any(), any(), any())
    }

    // === getStock ===

    @Test
    fun `getStock returns stock when found`() {
        // Arrange
        val stock = createStockEntity(quantity = 50)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        val result = stockService.getStock(storeId, productId)

        // Assert
        assertNotNull(result)
        assertEquals(50, result?.quantity)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `getStock returns null when not found`() {
        // Arrange
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

        // Act
        val result = stockService.getStock(storeId, productId)

        // Assert
        assertNull(result)
    }

    // === listStocks ===

    @Test
    fun `listStocks returns all stocks for store`() {
        // Arrange
        val stocks = listOf(createStockEntity(quantity = 100), createStockEntity(quantity = 5))
        whenever(stockRepository.listByStoreId(eq(storeId), any<Page>())).thenReturn(stocks)
        whenever(stockRepository.countByStoreId(storeId)).thenReturn(2L)

        // Act
        val (result, totalCount) = stockService.listStocks(storeId, lowStockOnly = false, page = 0, pageSize = 20)

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `listStocks with lowStockOnly returns only low stock items`() {
        // Arrange
        val lowStocks = listOf(createStockEntity(quantity = 3))
        whenever(stockRepository.listLowStock(eq(storeId), any<Page>())).thenReturn(lowStocks)
        whenever(stockRepository.countLowStock(storeId)).thenReturn(1L)

        // Act
        val (result, totalCount) = stockService.listStocks(storeId, lowStockOnly = true, page = 0, pageSize = 20)

        // Assert
        assertEquals(1, result.size)
        assertEquals(3, result[0].quantity)
        assertEquals(1L, totalCount)
    }

    @Test
    fun `listStocks returns empty list when no stocks exist`() {
        // Arrange
        whenever(stockRepository.listByStoreId(eq(storeId), any<Page>())).thenReturn(emptyList())
        whenever(stockRepository.countByStoreId(storeId)).thenReturn(0L)

        // Act
        val (result, totalCount) = stockService.listStocks(storeId, lowStockOnly = false, page = 0, pageSize = 20)

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    // === adjustStock: add ===

    @Test
    fun `adjustStock adds quantity to existing stock`() {
        // Arrange
        val existingStock = createStockEntity(quantity = 10)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        val result =
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = 5,
                movementType = "RECEIPT",
                referenceId = "ref-001",
                note = "Restock",
            )

        // Assert
        assertEquals(15, result.quantity)
        verify(stockRepository).persist(any<StockEntity>())
        verify(movementRepository).persist(any<StockMovementEntity>())
    }

    // === adjustStock: deduct ===

    @Test
    fun `adjustStock deducts quantity from existing stock`() {
        // Arrange
        val existingStock = createStockEntity(quantity = 20)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        val result =
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = -5,
                movementType = "SALE",
                referenceId = "txn-001",
                note = null,
            )

        // Assert
        assertEquals(15, result.quantity)
    }

    // === adjustStock: negative error ===

    @Test
    fun `adjustStock throws when result would be negative`() {
        // Arrange
        val existingStock = createStockEntity(quantity = 3)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)

        // Act & Assert
        val exception =
            assertThrows(IllegalArgumentException::class.java) {
                stockService.adjustStock(
                    storeId = storeId,
                    productId = productId,
                    quantityChange = -5,
                    movementType = "SALE",
                    referenceId = null,
                    note = null,
                )
            }
        assertEquals(
            "Stock quantity cannot be negative: current=3, change=-5",
            exception.message,
        )
    }

    // === adjustStock: new stock creation ===

    @Test
    fun `adjustStock creates new stock when not found`() {
        // Arrange
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        val result =
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = 10,
                movementType = "RECEIPT",
                referenceId = null,
                note = "Initial stock",
            )

        // Assert
        assertEquals(10, result.quantity)
        assertEquals(storeId, result.storeId)
        assertEquals(productId, result.productId)
        assertEquals(orgId, result.organizationId)
        verify(stockRepository).persist(any<StockEntity>())
    }

    @Test
    fun `adjustStock for new stock throws when initial quantity is negative`() {
        // Arrange
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = -1,
                movementType = "SALE",
                referenceId = null,
                note = null,
            )
        }
    }

    @Test
    fun `adjustStock throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = 5,
                movementType = "RECEIPT",
                referenceId = null,
                note = null,
            )
        }
    }

    @Test
    fun `adjustStock deducts to exactly zero`() {
        // Arrange
        val existingStock = createStockEntity(quantity = 5)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        val result =
            stockService.adjustStock(
                storeId = storeId,
                productId = productId,
                quantityChange = -5,
                movementType = "SALE",
                referenceId = null,
                note = null,
            )

        // Assert
        assertEquals(0, result.quantity)
    }

    // === adjustStock: StockLowEvent ===

    @Test
    fun `adjustStock publishes StockLowEvent when stock drops below threshold`() {
        // Arrange: stock at 15, threshold 10, deduct 6 -> new quantity 9 (below 10)
        val existingStock = createStockEntity(quantity = 15)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        stockService.adjustStock(
            storeId = storeId,
            productId = productId,
            quantityChange = -6,
            movementType = "SALE",
            referenceId = "txn-low",
            note = null,
        )

        // Assert
        verify(stockLowEventPublisher).publish(
            organizationId = eq(orgId),
            productId = eq(productId),
            storeId = eq(storeId),
            currentQuantity = eq(9),
            threshold = eq(10),
        )
    }

    @Test
    fun `adjustStock does not publish StockLowEvent when stock stays above threshold`() {
        // Arrange: stock at 20, threshold 10, deduct 5 -> new quantity 15 (above 10)
        val existingStock = createStockEntity(quantity = 20)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        stockService.adjustStock(
            storeId = storeId,
            productId = productId,
            quantityChange = -5,
            movementType = "SALE",
            referenceId = "txn-ok",
            note = null,
        )

        // Assert
        verify(stockLowEventPublisher, never()).publish(any(), any(), any(), any(), any())
    }

    @Test
    fun `adjustStock does not publish StockLowEvent when stock was already below threshold`() {
        // Arrange: stock at 5, threshold 10, deduct 2 -> new quantity 3 (was already below)
        val existingStock = createStockEntity(quantity = 5)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        stockService.adjustStock(
            storeId = storeId,
            productId = productId,
            quantityChange = -2,
            movementType = "SALE",
            referenceId = "txn-already-low",
            note = null,
        )

        // Assert
        verify(stockLowEventPublisher, never()).publish(any(), any(), any(), any(), any())
    }

    @Test
    fun `adjustStock publishes StockLowEvent when stock drops exactly to threshold`() {
        // Arrange: stock at 15, threshold 10, deduct 5 -> new quantity 10 (equal to threshold)
        val existingStock = createStockEntity(quantity = 15)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        stockService.adjustStock(
            storeId = storeId,
            productId = productId,
            quantityChange = -5,
            movementType = "SALE",
            referenceId = "txn-exact",
            note = null,
        )

        // Assert
        verify(stockLowEventPublisher).publish(
            organizationId = eq(orgId),
            productId = eq(productId),
            storeId = eq(storeId),
            currentQuantity = eq(10),
            threshold = eq(10),
        )
    }

    @Test
    fun `adjustStock does not publish StockLowEvent when adding stock`() {
        // Arrange: stock at 5, threshold 10, add 20 -> new quantity 25 (adding stock)
        val existingStock = createStockEntity(quantity = 5)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(existingStock)
        doNothing().whenever(stockRepository).persist(any<StockEntity>())
        doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

        // Act
        stockService.adjustStock(
            storeId = storeId,
            productId = productId,
            quantityChange = 20,
            movementType = "RECEIPT",
            referenceId = "rcpt-001",
            note = null,
        )

        // Assert
        verify(stockLowEventPublisher, never()).publish(any(), any(), any(), any(), any())
    }

    // === listMovements ===

    @Test
    fun `listMovements returns movements with pagination`() {
        // Arrange
        val movements = listOf(createMovementEntity(), createMovementEntity())
        whenever(
            movementRepository.listByStoreAndProduct(
                eq(storeId),
                eq(productId),
                isNull(),
                isNull(),
                any<Page>(),
            ),
        ).thenReturn(movements)
        whenever(
            movementRepository.countByStoreAndProduct(
                eq(storeId),
                eq(productId),
                isNull(),
                isNull(),
            ),
        ).thenReturn(2L)

        // Act
        val (result, totalCount) =
            stockService.listMovements(
                storeId = storeId,
                productId = productId,
                startDate = null,
                endDate = null,
                page = 0,
                pageSize = 20,
            )

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `listMovements with date range filters correctly`() {
        // Arrange
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val end = Instant.parse("2026-01-31T23:59:59Z")
        whenever(
            movementRepository.listByStoreAndProduct(
                eq(storeId),
                eq(null),
                eq(start),
                eq(end),
                any<Page>(),
            ),
        ).thenReturn(emptyList())
        whenever(
            movementRepository.countByStoreAndProduct(
                eq(storeId),
                eq(null),
                eq(start),
                eq(end),
            ),
        ).thenReturn(0L)

        // Act
        val (result, totalCount) =
            stockService.listMovements(
                storeId = storeId,
                productId = null,
                startDate = start,
                endDate = end,
                page = 0,
                pageSize = 20,
            )

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    // === Helpers ===

    private fun createStockEntity(quantity: Int = 0): StockEntity =
        StockEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StockServiceTest.storeId
            this.productId = this@StockServiceTest.productId
            this.quantity = quantity
            this.lowStockThreshold = 10
        }

    private fun createMovementEntity(): StockMovementEntity =
        StockMovementEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StockServiceTest.storeId
            this.productId = this@StockServiceTest.productId
            this.movementType = "SALE"
            this.quantity = -1
            this.referenceId = "txn-test"
            this.note = null
        }
}
