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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class StocktakeServiceTest {
    @Inject
    lateinit var stocktakeService: StocktakeService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

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

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class StartStocktake {
        @Test
        fun `棚卸しを開始する`() {
            // Arrange
            doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())

            // Act
            val result = stocktakeService.startStocktake(storeId)

            // Assert
            assertEquals(storeId, result.storeId)
            assertEquals("IN_PROGRESS", result.status)
            assertNotNull(result.startedAt)
            assertEquals(orgId, result.organizationId)
            verify(stocktakeRepository).persist(any<StocktakeEntity>())
        }
    }

    @Nested
    inner class RecordItem {
        @Test
        fun `新規棚卸し項目を記録する`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val stockEntity =
                StockEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.productId = productId
                    this.quantity = 10
                }
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(stockEntity)
            whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId)).thenReturn(null)
            doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

            // Act
            val result = stocktakeService.recordItem(stocktakeId, productId, actualQty = 8)

            // Assert
            assertEquals(stocktakeId, result.id)
            verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
        }

        @Test
        fun `既存棚卸し項目を更新する`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
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
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
            whenever(stockRepository.findByStoreAndProduct(storeId, productId)).thenReturn(null)
            whenever(stocktakeItemRepository.findByStocktakeAndProduct(stocktakeId, productId)).thenReturn(existingItem)
            doNothing().whenever(stocktakeItemRepository).persist(any<StocktakeItemEntity>())

            // Act
            val result = stocktakeService.recordItem(stocktakeId, productId, actualQty = 12)

            // Assert
            assertEquals(stocktakeId, result.id)
            assertEquals(12, existingItem.actualQty)
            assertEquals(0, existingItem.expectedQty)
            assertEquals(12, existingItem.difference)
            verify(stocktakeItemRepository).persist(any<StocktakeItemEntity>())
        }

        @Test
        fun `存在しない棚卸しへの記録は例外を投げる`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(null)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                stocktakeService.recordItem(stocktakeId, productId, actualQty = 5)
            }
        }

        @Test
        fun `完了済み棚卸しへの記録は例外を投げる`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.status = "COMPLETED"
                }
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                stocktakeService.recordItem(stocktakeId, productId, actualQty = 5)
            }
        }
    }

    @Nested
    inner class CompleteStocktake {
        @Test
        fun `差異がある項目の在庫を調整して棚卸しを完了する`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
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
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
            whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(listOf(item))
            doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())
            whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(
                StockEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.productId = productId
                    this.quantity = 8
                },
            )

            // Act
            val result = stocktakeService.completeStocktake(stocktakeId)

            // Assert
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
        fun `差異がゼロの項目は在庫調整しない`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val productId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.status = "IN_PROGRESS"
                }
            val item =
                StocktakeItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.stocktakeId = stocktakeId
                    this.productId = productId
                    this.expectedQty = 10
                    this.actualQty = 10
                    this.difference = 0
                }
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)
            whenever(stocktakeItemRepository.findByStocktakeId(stocktakeId)).thenReturn(listOf(item))
            doNothing().whenever(stocktakeRepository).persist(any<StocktakeEntity>())

            // Act
            val result = stocktakeService.completeStocktake(stocktakeId)

            // Assert
            assertEquals("COMPLETED", result.status)
            verify(stockService, never()).adjustStock(any(), any(), any(), any(), any(), any())
        }

        @Test
        fun `存在しない棚卸しの完了は例外を投げる`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(null)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                stocktakeService.completeStocktake(stocktakeId)
            }
        }

        @Test
        fun `完了済み棚卸しの再完了は例外を投げる`() {
            // Arrange
            val stocktakeId = UUID.randomUUID()
            val stocktake =
                StocktakeEntity().apply {
                    this.id = stocktakeId
                    this.organizationId = orgId
                    this.storeId = this@StocktakeServiceTest.storeId
                    this.status = "COMPLETED"
                }
            whenever(stocktakeRepository.findById(stocktakeId)).thenReturn(stocktake)

            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                stocktakeService.completeStocktake(stocktakeId)
            }
        }
    }
}
