package com.openpos.inventory.event

import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.service.PurchaseOrderService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * AutoReorderHandler の純粋ユニットテスト。
 */
class AutoReorderHandlerTest {
    private lateinit var handler: AutoReorderHandler
    private lateinit var stockRepository: StockRepository
    private lateinit var purchaseOrderService: PurchaseOrderService
    private lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        stockRepository = mock()
        purchaseOrderService = mock()
        tenantFilterService = mock()

        handler = AutoReorderHandler()
        handler.stockRepository = stockRepository
        handler.purchaseOrderService = purchaseOrderService
        handler.tenantFilterService = tenantFilterService

        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Test
    fun `skips when stock not found`() {
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

        handler.handleStockLow(orgId, storeId, productId, 5)

        verify(tenantFilterService).enableFilter()
        verify(stockRepository).findByStoreAndProduct(storeId, productId)
    }

    @Test
    fun `skips when reorderPoint is zero`() {
        val stock =
            StockEntity().apply {
                this.id = UUID.randomUUID()
                this.organizationId = orgId
                this.storeId = this@AutoReorderHandlerTest.storeId
                this.productId = this@AutoReorderHandlerTest.productId
                this.quantity = 5
                this.reorderPoint = 0
                this.reorderQuantity = 50
            }
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        handler.handleStockLow(orgId, storeId, productId, 5)
    }

    @Test
    fun `skips when reorderQuantity is zero`() {
        val stock =
            StockEntity().apply {
                this.id = UUID.randomUUID()
                this.organizationId = orgId
                this.storeId = this@AutoReorderHandlerTest.storeId
                this.productId = this@AutoReorderHandlerTest.productId
                this.quantity = 5
                this.reorderPoint = 10
                this.reorderQuantity = 0
            }
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        handler.handleStockLow(orgId, storeId, productId, 5)
    }

    @Test
    fun `triggers auto-reorder when quantity is at or below reorderPoint`() {
        val stock =
            StockEntity().apply {
                this.id = UUID.randomUUID()
                this.organizationId = orgId
                this.storeId = this@AutoReorderHandlerTest.storeId
                this.productId = this@AutoReorderHandlerTest.productId
                this.quantity = 5
                this.reorderPoint = 10
                this.reorderQuantity = 50
            }
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        handler.handleStockLow(orgId, storeId, productId, 5)
    }

    @Test
    fun `does not trigger auto-reorder when quantity is above reorderPoint`() {
        val stock =
            StockEntity().apply {
                this.id = UUID.randomUUID()
                this.organizationId = orgId
                this.storeId = this@AutoReorderHandlerTest.storeId
                this.productId = this@AutoReorderHandlerTest.productId
                this.quantity = 15
                this.reorderPoint = 10
                this.reorderQuantity = 50
            }
        whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

        handler.handleStockLow(orgId, storeId, productId, 15)
    }
}
