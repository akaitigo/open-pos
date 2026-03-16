package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StocktakeEntity
import com.openpos.inventory.entity.StocktakeItemEntity
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.repository.StocktakeItemRepository
import com.openpos.inventory.repository.StocktakeRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

@QuarkusTest
class StocktakeServiceTest {
    @Inject
    lateinit var stocktakeService: StocktakeService

    @InjectMock
    lateinit var stocktakeRepository: StocktakeRepository

    @InjectMock
    lateinit var stocktakeItemRepository: StocktakeItemRepository

    @InjectMock
    lateinit var stockRepository: StockRepository

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

    // === startStocktake ===

    @Test
    fun `startStocktake creates new stocktake with IN_PROGRESS status`() {
        // Arrange
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())

        // Act
        val result = stocktakeService.startStocktake(storeId)

        // Assert
        assertEquals(orgId, result.organizationId)
        assertEquals(storeId, result.storeId)
        assertEquals("IN_PROGRESS", result.status)
        assertNotNull(result.startedAt)
        verify(stocktakeRepository).persist(any<StocktakeEntity>())
    }

    @Test
    fun `startStocktake throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.startStocktake(storeId)
        }
    }

    // === recordItem: new item ===

    @Test
    fun `recordItem creates new item when not existing`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val stock = createStockEntity(productId1, quantity = 100)

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId1)).thenReturn(stock)
        whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId1)).thenReturn(null)
        doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

        // Act
        val result = stocktakeService.recordItem(stocktakeId, productId1, actualQty = 95)

        // Assert
        assertEquals(stocktakeId, result.id)
        verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `recordItem calculates difference correctly for new item`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val stock = createStockEntity(productId1, quantity = 50)

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId1)).thenReturn(stock)
        whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId1)).thenReturn(null)
        doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

        // Act
        stocktakeService.recordItem(stocktakeId, productId1, actualQty = 45)

        // Assert - verify persist was called (difference = 45 - 50 = -5)
        verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
    }

    // === recordItem: update existing ===

    @Test
    fun `recordItem updates existing item when already recorded`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val stock = createStockEntity(productId1, quantity = 100)
        val existingItem = createStocktakeItemEntity(stocktakeId, productId1, expectedQty = 100, actualQty = 90)

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId1)).thenReturn(stock)
        whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId1)).thenReturn(existingItem)
        doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

        // Act
        stocktakeService.recordItem(stocktakeId, productId1, actualQty = 98)

        // Assert
        assertEquals(98, existingItem.actualQty)
        assertEquals(100, existingItem.expectedQty)
        assertEquals(-2, existingItem.difference)
        verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
    }

    // === recordItem: edge cases ===

    @Test
    fun `recordItem uses zero as expectedQty when stock not found`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId1)).thenReturn(null)
        whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId1)).thenReturn(null)
        doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

        // Act
        stocktakeService.recordItem(stocktakeId, productId1, actualQty = 5)

        // Assert
        verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
    }

    @Test
    fun `recordItem throws when stocktake not found`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(null)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.recordItem(stocktakeId, productId1, actualQty = 10)
        }
    }

    @Test
    fun `recordItem throws when stocktake is not in progress`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "COMPLETED")
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.recordItem(stocktakeId, productId1, actualQty = 10)
        }
    }

    @Test
    fun `recordItem throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.recordItem(UUID.randomUUID(), productId1, actualQty = 10)
        }
    }

    // === completeStocktake ===

    @Test
    fun `completeStocktake adjusts stock for items with difference`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val items =
            listOf(
                createStocktakeItemEntity(stocktakeId, productId1, expectedQty = 100, actualQty = 95),
                createStocktakeItemEntity(stocktakeId, productId2, expectedQty = 50, actualQty = 50),
            )
        // item1: difference = -5, item2: difference = 0

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(items)
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())
        whenever(
            stockService.adjustStock(
                storeId = eq(storeId),
                productId = eq(productId1),
                quantityChange = eq(-5),
                movementType = eq("ADJUSTMENT"),
                referenceId = any(),
                note = any(),
            ),
        ).thenReturn(createStockEntity(productId1, 95))

        // Act
        val result = stocktakeService.completeStocktake(stocktakeId)

        // Assert
        assertEquals("COMPLETED", result.status)
        assertNotNull(result.completedAt)
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(-5),
            movementType = eq("ADJUSTMENT"),
            referenceId = any(),
            note = any(),
        )
    }

    @Test
    fun `completeStocktake skips items with zero difference`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val items =
            listOf(
                createStocktakeItemEntity(stocktakeId, productId1, expectedQty = 50, actualQty = 50),
            )

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(items)
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())

        // Act
        stocktakeService.completeStocktake(stocktakeId)

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

    @Test
    fun `completeStocktake sets status to COMPLETED`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(emptyList())
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())

        // Act
        val result = stocktakeService.completeStocktake(stocktakeId)

        // Assert
        assertEquals("COMPLETED", result.status)
        assertNotNull(result.completedAt)
        verify(stocktakeRepository).persist(any<StocktakeEntity>())
    }

    @Test
    fun `completeStocktake throws when stocktake not found`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(null)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.completeStocktake(stocktakeId)
        }
    }

    @Test
    fun `completeStocktake throws when stocktake is not in progress`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "COMPLETED")
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.completeStocktake(stocktakeId)
        }
    }

    @Test
    fun `completeStocktake adjusts stock with positive difference`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        val items =
            listOf(
                createStocktakeItemEntity(stocktakeId, productId1, expectedQty = 30, actualQty = 35),
            )

        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(items)
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())
        whenever(
            stockService.adjustStock(
                storeId = eq(storeId),
                productId = eq(productId1),
                quantityChange = eq(5),
                movementType = eq("ADJUSTMENT"),
                referenceId = any(),
                note = any(),
            ),
        ).thenReturn(createStockEntity(productId1, 35))

        // Act
        stocktakeService.completeStocktake(stocktakeId)

        // Assert
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(5),
            movementType = eq("ADJUSTMENT"),
            referenceId = any(),
            note = any(),
        )
    }

    // === getStocktake ===

    @Test
    fun `getStocktake returns stocktake when found`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val stocktake = createStocktakeEntity(stocktakeId, status = "IN_PROGRESS")
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)

        // Act
        val result = stocktakeService.getStocktake(stocktakeId)

        // Assert
        assertEquals(stocktakeId, result.id)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `getStocktake throws when not found`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(null)

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stocktakeService.getStocktake(stocktakeId)
        }
    }

    // === getStocktakeItems ===

    @Test
    fun `getStocktakeItems returns items for stocktake`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        val items =
            listOf(
                createStocktakeItemEntity(stocktakeId, productId1, expectedQty = 100, actualQty = 98),
                createStocktakeItemEntity(stocktakeId, productId2, expectedQty = 50, actualQty = 50),
            )
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(items)

        // Act
        val result = stocktakeService.getStocktakeItems(stocktakeId)

        // Assert
        assertEquals(2, result.size)
    }

    @Test
    fun `getStocktakeItems returns empty list when no items exist`() {
        // Arrange
        val stocktakeId = UUID.randomUUID()
        whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(emptyList())

        // Act
        val result = stocktakeService.getStocktakeItems(stocktakeId)

        // Assert
        assertEquals(0, result.size)
    }

    // === Helpers ===

    private fun createStocktakeEntity(
        id: UUID = UUID.randomUUID(),
        status: String = "IN_PROGRESS",
    ): StocktakeEntity =
        StocktakeEntity().apply {
            this.id = id
            this.organizationId = orgId
            this.storeId = this@StocktakeServiceTest.storeId
            this.status = status
            this.startedAt = Instant.now()
        }

    private fun createStocktakeItemEntity(
        stocktakeId: UUID,
        productId: UUID,
        expectedQty: Int,
        actualQty: Int,
    ): StocktakeItemEntity =
        StocktakeItemEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.stocktakeId = stocktakeId
            this.productId = productId
            this.expectedQty = expectedQty
            this.actualQty = actualQty
            this.difference = actualQty - expectedQty
        }

    private fun createStockEntity(
        productId: UUID,
        quantity: Int,
    ): StockEntity =
        StockEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StocktakeServiceTest.storeId
            this.productId = productId
            this.quantity = quantity
            this.lowStockThreshold = 10
        }
}
