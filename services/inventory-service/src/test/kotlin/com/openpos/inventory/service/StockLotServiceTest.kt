package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockLotEntity
import com.openpos.inventory.repository.StockLotRepository
import io.quarkus.panache.common.Page
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class StockLotServiceTest {
    @Inject
    lateinit var stockLotService: StockLotService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var stockLotRepository: StockLotRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class ListByStoreAndProduct {
        @Test
        fun `店舗と商品でロット一覧を取得する`() {
            // Arrange
            val lot1 =
                StockLotEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StockLotServiceTest.storeId
                    this.productId = this@StockLotServiceTest.productId
                    this.lotNumber = "LOT-001"
                    this.quantity = 50
                    this.expiryDate = LocalDate.of(2026, 6, 30)
                }
            whenever(stockLotRepository.listByStoreAndProduct(eq(storeId), eq(productId), any<Page>()))
                .thenReturn(listOf(lot1))
            whenever(stockLotRepository.countByStoreAndProduct(storeId, productId)).thenReturn(1L)

            // Act
            val (lots, count) = stockLotService.listByStoreAndProduct(storeId, productId, 0, 20)

            // Assert
            assertEquals(1, lots.size)
            assertEquals("LOT-001", lots[0].lotNumber)
            assertEquals(1L, count)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `ロットが存在しない場合は空リストを返す`() {
            // Arrange
            whenever(stockLotRepository.listByStoreAndProduct(eq(storeId), eq(productId), any<Page>()))
                .thenReturn(emptyList())
            whenever(stockLotRepository.countByStoreAndProduct(storeId, productId)).thenReturn(0L)

            // Act
            val (lots, count) = stockLotService.listByStoreAndProduct(storeId, productId, 0, 20)

            // Assert
            assertEquals(0, lots.size)
            assertEquals(0L, count)
        }
    }

    @Nested
    inner class ListExpiringSoon {
        @Test
        fun `期限切れ間近のロットを取得する`() {
            // Arrange
            val lot =
                StockLotEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.storeId = this@StockLotServiceTest.storeId
                    this.productId = this@StockLotServiceTest.productId
                    this.lotNumber = "EXPIRING"
                    this.quantity = 10
                    this.expiryDate = LocalDate.now().plusDays(3)
                }
            whenever(stockLotRepository.findExpiringSoon(eq(7), any<Page>())).thenReturn(listOf(lot))
            whenever(stockLotRepository.countExpiringSoon(7)).thenReturn(1L)

            // Act
            val (lots, count) = stockLotService.listExpiringSoon(daysAhead = 7, page = 0, pageSize = 20)

            // Assert
            assertEquals(1, lots.size)
            assertEquals("EXPIRING", lots[0].lotNumber)
            assertEquals(1L, count)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `期限切れ間近のロットがない場合は空リストを返す`() {
            // Arrange
            whenever(stockLotRepository.findExpiringSoon(eq(7), any<Page>())).thenReturn(emptyList())
            whenever(stockLotRepository.countExpiringSoon(7)).thenReturn(0L)

            // Act
            val (lots, count) = stockLotService.listExpiringSoon(daysAhead = 7, page = 0, pageSize = 20)

            // Assert
            assertEquals(0, lots.size)
            assertEquals(0L, count)
        }
    }

    @Nested
    inner class Create {
        @Test
        fun `ロットを全フィールド指定で作成する`() {
            // Arrange
            doNothing().whenever(stockLotRepository).persist(any<StockLotEntity>())

            // Act
            val result =
                stockLotService.create(
                    storeId = storeId,
                    productId = productId,
                    lotNumber = "LOT-NEW-001",
                    quantity = 100,
                    expiryDate = LocalDate.of(2027, 12, 31),
                )

            // Assert
            assertEquals(storeId, result.storeId)
            assertEquals(productId, result.productId)
            assertEquals("LOT-NEW-001", result.lotNumber)
            assertEquals(100, result.quantity)
            assertEquals(LocalDate.of(2027, 12, 31), result.expiryDate)
            assertEquals(orgId, result.organizationId)
            assertNotNull(result.receivedAt)
            verify(stockLotRepository).persist(any<StockLotEntity>())
        }

        @Test
        fun `ロット番号と期限なしでも作成できる`() {
            // Arrange
            doNothing().whenever(stockLotRepository).persist(any<StockLotEntity>())

            // Act
            val result =
                stockLotService.create(
                    storeId = storeId,
                    productId = productId,
                    lotNumber = null,
                    quantity = 50,
                    expiryDate = null,
                )

            // Assert
            assertNull(result.lotNumber)
            assertNull(result.expiryDate)
            assertEquals(50, result.quantity)
            verify(stockLotRepository).persist(any<StockLotEntity>())
        }
    }
}
