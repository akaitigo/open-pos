package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.entity.StocktakeEntity
import com.openpos.inventory.entity.StocktakeItemEntity
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.repository.StocktakeItemRepository
import com.openpos.inventory.repository.StocktakeRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
import java.util.UUID

/**
 * StocktakeService の純粋ユニットテスト。
 * CDI プロキシを回避して JaCoCo カバレッジを確保する。
 */
class StocktakeServiceUnitTest {
    private lateinit var service: StocktakeService
    private lateinit var stocktakeRepository: StocktakeRepository
    private lateinit var stocktakeItemRepository: StocktakeItemRepository
    private lateinit var stockRepository: StockRepository
    private lateinit var stockService: StockService
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        stocktakeRepository = mock()
        stocktakeItemRepository = mock()
        stockRepository = mock()
        stockService = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = StocktakeService()
        service.stocktakeRepository = stocktakeRepository
        service.stocktakeItemRepository = stocktakeItemRepository
        service.stockRepository = stockRepository
        service.stockService = stockService
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())
        doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
    }

    @Nested
    inner class StartStocktake {
        @Test
        fun `creates IN_PROGRESS stocktake`() {
            val result = service.startStocktake(storeId)

            assertEquals(storeId, result.storeId)
            assertEquals("IN_PROGRESS", result.status)
            assertEquals(orgId, result.organizationId)
            assertNotNull(result.startedAt)
            verify(stocktakeRepository).persist(any<StocktakeEntity>())
        }
    }

    @Nested
    inner class RecordItem {
        @Test
        fun `records new item`() {
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val stockEntity =
                StockEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.productId = productId
                    this.quantity = 10
                }
            val mockQuery1 = mock<PanacheQuery<StocktakeEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(stocktake)
            whenever(stocktakeRepository.find(eq("id = ?1"), eq(stocktakeId))).thenReturn(mockQuery1)
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stockEntity)
            whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId)).thenReturn(null)

            val result = service.recordItem(stocktakeId, productId, 8)

            assertEquals(stocktakeId, result.id)
            verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
        }

        @Test
        fun `updates existing item`() {
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val existingItem =
                StocktakeItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.stocktakeId = stocktakeId
                    this.productId = productId
                    this.expectedQty = 10
                    this.actualQty = 5
                    this.difference = -5
                }
            val mockQuery2 = mock<PanacheQuery<StocktakeEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(stocktake)
            whenever(stocktakeRepository.find(eq("id = ?1"), eq(stocktakeId))).thenReturn(mockQuery2)
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)
            whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId)).thenReturn(existingItem)

            service.recordItem(stocktakeId, productId, 12)

            assertEquals(12, existingItem.actualQty)
            assertEquals(0, existingItem.expectedQty)
            assertEquals(12, existingItem.difference)
        }

        @Test
        fun `throws when stocktake not found`() {
            val notFoundQuery = mock<PanacheQuery<StocktakeEntity>>()
            whenever(notFoundQuery.firstResult()).thenReturn(null)
            whenever(stocktakeRepository.find(eq("id = ?1"), any<UUID>())).thenReturn(notFoundQuery)

            assertThrows(IllegalArgumentException::class.java) {
                service.recordItem(UUID.randomUUID(), UUID.randomUUID(), 5)
            }
        }

        @Test
        fun `throws when stocktake is not in progress`() {
            val stocktakeId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "COMPLETED"
                }
            val mockQuery3 = mock<PanacheQuery<StocktakeEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(stocktake)
            whenever(stocktakeRepository.find(eq("id = ?1"), eq(stocktakeId))).thenReturn(mockQuery3)

            assertThrows(IllegalArgumentException::class.java) {
                service.recordItem(stocktakeId, UUID.randomUUID(), 5)
            }
        }
    }

    @Nested
    inner class CompleteStocktake {
        @Test
        fun `adjusts stock for items with difference and completes`() {
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val item =
                StocktakeItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.stocktakeId = stocktakeId
                    this.productId = productId
                    this.expectedQty = 10
                    this.actualQty = 8
                    this.difference = -2
                }
            val mockQuery4 = mock<PanacheQuery<StocktakeEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(stocktake)
            whenever(stocktakeRepository.find(eq("id = ?1"), eq(stocktakeId))).thenReturn(mockQuery4)
            whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(listOf(item))
            whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(mock())

            val result = service.completeStocktake(stocktakeId)

            assertEquals("COMPLETED", result.status)
            assertNotNull(result.completedAt)
            verify(stockService).adjustStock(
                storeId = eq(storeId),
                productId = eq(productId),
                quantityChange = eq(-2),
                movementType = eq("ADJUSTMENT"),
                referenceId = eq(stocktakeId.toString()),
                note = eq("棚卸し差異調整"),
            )
        }

        @Test
        fun `skips adjustment for zero difference`() {
            val stocktakeId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val item =
                StocktakeItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.stocktakeId = stocktakeId
                    this.productId = UUID.randomUUID()
                    this.expectedQty = 10
                    this.actualQty = 10
                    this.difference = 0
                }
            val mockQuery5 = mock<PanacheQuery<StocktakeEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(stocktake)
            whenever(stocktakeRepository.find(eq("id = ?1"), eq(stocktakeId))).thenReturn(mockQuery5)
            whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(listOf(item))

            val result = service.completeStocktake(stocktakeId)

            assertEquals("COMPLETED", result.status)
            verify(stockService, never()).adjustStock(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `throws when stocktake not found`() {
            val notFoundQuery = mock<PanacheQuery<StocktakeEntity>>()
            whenever(notFoundQuery.firstResult()).thenReturn(null)
            whenever(stocktakeRepository.find(eq("id = ?1"), any<UUID>())).thenReturn(notFoundQuery)

            assertThrows(IllegalArgumentException::class.java) {
                service.completeStocktake(UUID.randomUUID())
            }
        }
    }

    @Nested
    inner class GetStocktake {
        @Test
        fun `returns stocktake with items`() {
            val stocktakeId = UUID.randomUUID()
            val entity =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceUnitTest.storeId
                    this.status = "IN_PROGRESS"
                }
            whenever(stocktakeRepository.findByIdWithItems(stocktakeId)).thenReturn(entity)

            val result = service.getStocktake(stocktakeId)

            assertEquals(stocktakeId, result.id)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `throws when not found`() {
            whenever(stocktakeRepository.findByIdWithItems(any<UUID>())).thenReturn(null)

            assertThrows(IllegalArgumentException::class.java) {
                service.getStocktake(UUID.randomUUID())
            }
        }
    }
}
