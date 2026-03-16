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
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@QuarkusTest
class StockLotServiceTest {
    @Inject
    lateinit var stockLotService: StockLotService

    @InjectMock
    lateinit var stockLotRepository: StockLotRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === listByStoreAndProduct ===

    @Test
    fun `listByStoreAndProduct returns lots with pagination`() {
        // Arrange
        val lots =
            listOf(
                createLotEntity(quantity = 50, expiryDate = LocalDate.of(2026, 6, 1)),
                createLotEntity(quantity = 30, expiryDate = LocalDate.of(2026, 7, 1)),
            )
        whenever(stockLotRepository.listByStoreAndProduct(eq(storeId), eq(productId), any<Page>())).thenReturn(lots)
        whenever(stockLotRepository.countByStoreAndProduct(storeId, productId)).thenReturn(2L)

        // Act
        val (result, totalCount) = stockLotService.listByStoreAndProduct(storeId, productId, page = 0, pageSize = 20)

        // Assert
        assertEquals(2, result.size)
        assertEquals(2L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `listByStoreAndProduct returns empty list when no lots exist`() {
        // Arrange
        whenever(stockLotRepository.listByStoreAndProduct(eq(storeId), eq(productId), any<Page>())).thenReturn(emptyList())
        whenever(stockLotRepository.countByStoreAndProduct(storeId, productId)).thenReturn(0L)

        // Act
        val (result, totalCount) = stockLotService.listByStoreAndProduct(storeId, productId, page = 0, pageSize = 20)

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    @Test
    fun `listByStoreAndProduct enables tenant filter`() {
        // Arrange
        whenever(stockLotRepository.listByStoreAndProduct(eq(storeId), eq(productId), any<Page>())).thenReturn(emptyList())
        whenever(stockLotRepository.countByStoreAndProduct(storeId, productId)).thenReturn(0L)

        // Act
        stockLotService.listByStoreAndProduct(storeId, productId, page = 0, pageSize = 10)

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    // === listExpiringSoon ===

    @Test
    fun `listExpiringSoon returns lots expiring within days`() {
        // Arrange
        val lots =
            listOf(
                createLotEntity(quantity = 20, expiryDate = LocalDate.now().plusDays(3)),
            )
        whenever(stockLotRepository.findExpiringSoon(eq(7), any<Page>())).thenReturn(lots)
        whenever(stockLotRepository.countExpiringSoon(7)).thenReturn(1L)

        // Act
        val (result, totalCount) = stockLotService.listExpiringSoon(daysAhead = 7, page = 0, pageSize = 20)

        // Assert
        assertEquals(1, result.size)
        assertEquals(1L, totalCount)
        verify(tenantFilterService).enableFilter()
    }

    @Test
    fun `listExpiringSoon returns empty list when no lots are expiring`() {
        // Arrange
        whenever(stockLotRepository.findExpiringSoon(eq(7), any<Page>())).thenReturn(emptyList())
        whenever(stockLotRepository.countExpiringSoon(7)).thenReturn(0L)

        // Act
        val (result, totalCount) = stockLotService.listExpiringSoon(daysAhead = 7, page = 0, pageSize = 20)

        // Assert
        assertEquals(0, result.size)
        assertEquals(0L, totalCount)
    }

    @Test
    fun `listExpiringSoon enables tenant filter`() {
        // Arrange
        whenever(stockLotRepository.findExpiringSoon(eq(30), any<Page>())).thenReturn(emptyList())
        whenever(stockLotRepository.countExpiringSoon(30)).thenReturn(0L)

        // Act
        stockLotService.listExpiringSoon(daysAhead = 30, page = 0, pageSize = 10)

        // Assert
        verify(tenantFilterService).enableFilter()
    }

    // === create ===

    @Test
    fun `create persists lot entity with all fields`() {
        // Arrange
        doNothing().whenever(stockLotRepository).persist(any<StockLotEntity>())
        val expiryDate = LocalDate.of(2026, 12, 31)

        // Act
        val result =
            stockLotService.create(
                storeId = storeId,
                productId = productId,
                lotNumber = "LOT-001",
                quantity = 100,
                expiryDate = expiryDate,
            )

        // Assert
        assertEquals(orgId, result.organizationId)
        assertEquals(storeId, result.storeId)
        assertEquals(productId, result.productId)
        assertEquals("LOT-001", result.lotNumber)
        assertEquals(100, result.quantity)
        assertEquals(expiryDate, result.expiryDate)
        assertNotNull(result.receivedAt)
        verify(stockLotRepository).persist(any<StockLotEntity>())
    }

    @Test
    fun `create allows null lotNumber`() {
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
    }

    @Test
    fun `create allows null expiryDate`() {
        // Arrange
        doNothing().whenever(stockLotRepository).persist(any<StockLotEntity>())

        // Act
        val result =
            stockLotService.create(
                storeId = storeId,
                productId = productId,
                lotNumber = "LOT-002",
                quantity = 25,
                expiryDate = null,
            )

        // Assert
        assertNull(result.expiryDate)
        assertEquals("LOT-002", result.lotNumber)
    }

    @Test
    fun `create throws when organizationId is not set`() {
        // Arrange
        organizationIdHolder.organizationId = null

        // Act & Assert
        assertThrows(IllegalArgumentException::class.java) {
            stockLotService.create(
                storeId = storeId,
                productId = productId,
                lotNumber = "LOT-003",
                quantity = 10,
                expiryDate = null,
            )
        }
    }

    @Test
    fun `create sets organizationId from holder`() {
        // Arrange
        doNothing().whenever(stockLotRepository).persist(any<StockLotEntity>())

        // Act
        val result =
            stockLotService.create(
                storeId = storeId,
                productId = productId,
                lotNumber = null,
                quantity = 5,
                expiryDate = null,
            )

        // Assert
        assertEquals(orgId, result.organizationId)
    }

    // === Helpers ===

    private fun createLotEntity(
        quantity: Int = 0,
        expiryDate: LocalDate? = null,
    ): StockLotEntity =
        StockLotEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@StockLotServiceTest.storeId
            this.productId = this@StockLotServiceTest.productId
            this.lotNumber = "LOT-TEST"
            this.quantity = quantity
            this.expiryDate = expiryDate
            this.receivedAt = Instant.now()
        }
}
