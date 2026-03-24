package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StockMovementEntity
import com.openpos.inventory.event.StockLowEventPublisher
import com.openpos.inventory.repository.StockMovementRepository
import com.openpos.inventory.repository.StockRepository
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * StockService の純粋ユニットテスト（CDIプロキシなし）。
 * JaCoCo が確実にカバレッジを計測できるようにする。
 */
class StockServiceUnitTest {
    private lateinit var stockService: StockService
    private lateinit var stockRepository: StockRepository
    private lateinit var movementRepository: StockMovementRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder
    private lateinit var stockLowEventPublisher: StockLowEventPublisher

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        stockRepository = mock()
        movementRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        stockLowEventPublisher = mock()

        stockService = StockService()
        stockService.stockRepository = stockRepository
        stockService.movementRepository = movementRepository
        stockService.tenantFilterService = tenantFilterService
        stockService.organizationIdHolder = organizationIdHolder
        stockService.stockLowEventPublisher = stockLowEventPublisher
        stockService.defaultLowStockThreshold = 10

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class GetStock {
        @Test
        fun `returns stock when found`() {
            val stock = createStockEntity(quantity = 50)
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stock)

            val result = stockService.getStock(storeId, productId)

            assertNotNull(result)
            assertEquals(50, result?.quantity)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)

            val result = stockService.getStock(storeId, productId)

            assertNull(result)
        }
    }

    @Nested
    inner class ListStocks {
        @Test
        fun `returns all stocks for store`() {
            val stocks = listOf(createStockEntity(quantity = 100), createStockEntity(quantity = 5))
            whenever(stockRepository.listByStoreId(eq(storeId), any<Page>())).thenReturn(stocks)
            whenever(stockRepository.countByStoreId(storeId)).thenReturn(2L)

            val (result, totalCount) = stockService.listStocks(storeId, lowStockOnly = false, page = 0, pageSize = 20)

            assertEquals(2, result.size)
            assertEquals(2L, totalCount)
        }

        @Test
        fun `returns only low stock items when lowStockOnly is true`() {
            val lowStocks = listOf(createStockEntity(quantity = 3))
            whenever(stockRepository.listLowStock(eq(storeId), any<Page>())).thenReturn(lowStocks)
            whenever(stockRepository.countLowStock(storeId)).thenReturn(1L)

            val (result, totalCount) = stockService.listStocks(storeId, lowStockOnly = true, page = 0, pageSize = 20)

            assertEquals(1, result.size)
            assertEquals(3, result[0].quantity)
            assertEquals(1L, totalCount)
        }
    }

    @Nested
    inner class AdjustStock {
        @Test
        fun `adds quantity to existing stock`() {
            val existingStock = createStockEntity(quantity = 10)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, 5, "RECEIPT", "ref-001", "Restock")

            assertEquals(15, result.quantity)
            verify(stockRepository).persist(any<StockEntity>())
            verify(movementRepository).persist(any<StockMovementEntity>())
        }

        @Test
        fun `deducts quantity from existing stock`() {
            val existingStock = createStockEntity(quantity = 20)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, -5, "SALE", "txn-001", null)

            assertEquals(15, result.quantity)
        }

        @Test
        fun `throws InsufficientStockException when result would be negative`() {
            val existingStock = createStockEntity(quantity = 3)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)

            assertThrows(InsufficientStockException::class.java) {
                stockService.adjustStock(storeId, productId, -5, "SALE", null, null)
            }
        }

        @Test
        fun `creates new stock when not found`() {
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(null)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, 10, "RECEIPT", null, "Initial stock")

            assertEquals(10, result.quantity)
            assertEquals(storeId, result.storeId)
            assertEquals(productId, result.productId)
            assertEquals(orgId, result.organizationId)
        }

        @Test
        fun `throws when organizationId is not set`() {
            organizationIdHolder.organizationId = null

            assertThrows(IllegalArgumentException::class.java) {
                stockService.adjustStock(storeId, productId, 5, "RECEIPT", null, null)
            }
        }

        @Test
        fun `deducts to exactly zero`() {
            val existingStock = createStockEntity(quantity = 5)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, -5, "SALE", null, null)

            assertEquals(0, result.quantity)
        }

        @Test
        fun `publishes StockLowEvent when stock drops below threshold`() {
            val existingStock = createStockEntity(quantity = 15)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, -6, "SALE", "txn-low", null)

            assertEquals(9, result.quantity)
            verify(stockLowEventPublisher).publish(
                organizationId = eq(orgId),
                productId = eq(productId),
                storeId = eq(storeId),
                currentQuantity = eq(9),
                threshold = eq(10),
            )
        }

        @Test
        fun `does not publish StockLowEvent when stock stays above threshold`() {
            val existingStock = createStockEntity(quantity = 20)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, -5, "SALE", "txn-ok", null)

            assertEquals(15, result.quantity)
            verify(stockLowEventPublisher, never()).publish(any(), any(), any(), any(), any())
        }

        @Test
        fun `does not publish StockLowEvent when stock was already below threshold`() {
            val existingStock = createStockEntity(quantity = 5)
            whenever(stockRepository.findByStoreAndProductForUpdate(storeId, productId)).thenReturn(existingStock)
            doNothing().whenever(stockRepository).persist(any<StockEntity>())
            doNothing().whenever(movementRepository).persist(any<StockMovementEntity>())

            val result = stockService.adjustStock(storeId, productId, -2, "SALE", null, null)

            assertEquals(3, result.quantity)
            verify(stockLowEventPublisher, never()).publish(any(), any(), any(), any(), any())
        }
    }

    @Nested
    inner class ListMovements {
        @Test
        fun `returns movements with pagination`() {
            val movements = listOf(createMovementEntity(), createMovementEntity())
            whenever(
                movementRepository.listByStoreAndProduct(eq(storeId), eq(productId), eq(null), eq(null), any<Page>()),
            ).thenReturn(movements)
            whenever(
                movementRepository.countByStoreAndProduct(eq(storeId), eq(productId), eq(null), eq(null)),
            ).thenReturn(2L)

            val (result, totalCount) = stockService.listMovements(storeId, productId, null, null, 0, 20)

            assertEquals(2, result.size)
            assertEquals(2L, totalCount)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns movements with date range`() {
            val start = Instant.parse("2026-01-01T00:00:00Z")
            val end = Instant.parse("2026-01-31T23:59:59Z")
            whenever(
                movementRepository.listByStoreAndProduct(eq(storeId), eq(null), eq(start), eq(end), any<Page>()),
            ).thenReturn(emptyList())
            whenever(
                movementRepository.countByStoreAndProduct(eq(storeId), eq(null), eq(start), eq(end)),
            ).thenReturn(0L)

            val (result, totalCount) = stockService.listMovements(storeId, null, start, end, 0, 20)

            assertEquals(0, result.size)
            assertEquals(0L, totalCount)
        }
    }

    private fun createStockEntity(quantity: Int = 0): StockEntity =
        StockEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StockServiceUnitTest.storeId
            this.productId = this@StockServiceUnitTest.productId
            this.quantity = quantity
            this.lowStockThreshold = 10
        }

    private fun createMovementEntity(): StockMovementEntity =
        StockMovementEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StockServiceUnitTest.storeId
            this.productId = this@StockServiceUnitTest.productId
            this.movementType = "SALE"
            this.quantity = -1
        }
}
