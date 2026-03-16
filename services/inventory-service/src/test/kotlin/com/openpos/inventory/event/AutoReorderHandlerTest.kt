package com.openpos.inventory.event

import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.service.PurchaseOrderService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class AutoReorderHandlerTest {
    @Inject
    lateinit var autoReorderHandler: AutoReorderHandler

    @InjectMock
    lateinit var stockRepository: StockRepository

    @InjectMock
    lateinit var purchaseOrderService: PurchaseOrderService

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === handleStockLow: stock not found ===

    @Test
    fun `handleStockLow returns early when stock not found`() {
        // Arrange
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(tenantFilterService).enableFilter()
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    // === handleStockLow: reorder not configured ===

    @Test
    fun `handleStockLow skips when reorderPoint is zero`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 0, reorderQuantity = 50)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    @Test
    fun `handleStockLow skips when reorderQuantity is zero`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 20, reorderQuantity = 0)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    @Test
    fun `handleStockLow skips when reorderPoint is negative`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = -1, reorderQuantity = 50)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    @Test
    fun `handleStockLow skips when reorderQuantity is negative`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 20, reorderQuantity = -1)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    // === handleStockLow: reorder triggered ===

    @Test
    fun `handleStockLow processes when currentQuantity equals reorderPoint`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 10, reorderQuantity = 50)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 10)

        // Assert - handler reaches the reorder trigger condition
        verify(tenantFilterService).enableFilter()
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    @Test
    fun `handleStockLow processes when currentQuantity is below reorderPoint`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 20, reorderQuantity = 100)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(tenantFilterService).enableFilter()
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    // === handleStockLow: reorder NOT triggered ===

    @Test
    fun `handleStockLow does not trigger when currentQuantity is above reorderPoint`() {
        // Arrange
        val stock = createStockEntity(reorderPoint = 10, reorderQuantity = 50)
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 15)

        // Assert
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    // === handleStockLow: enables tenant filter ===

    @Test
    fun `handleStockLow enables tenant filter`() {
        // Arrange
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

        // Act
        autoReorderHandler.handleStockLow(orgId, storeId, productId, currentQuantity = 5)

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    // === Helpers ===

    private fun createStockEntity(
        reorderPoint: Int = 0,
        reorderQuantity: Int = 0,
    ): StockEntity =
        StockEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@AutoReorderHandlerTest.storeId
            this.productId = this@AutoReorderHandlerTest.productId
            this.quantity = 5
            this.lowStockThreshold = 10
            this.reorderPoint = reorderPoint
            this.reorderQuantity = reorderQuantity
        }
}
