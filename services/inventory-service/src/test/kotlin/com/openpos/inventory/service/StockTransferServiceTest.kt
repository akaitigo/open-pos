package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.StockTransferEntity
import com.openpos.inventory.repository.StockTransferRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class StockTransferServiceTest {
    private lateinit var service: StockTransferService
    private val stockTransferRepository = mock<StockTransferRepository>()
    private val tenantFilterService = mock<TenantFilterService>()
    private val organizationIdHolder = mock<OrganizationIdHolder>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            StockTransferService().apply {
                this.stockTransferRepository = this@StockTransferServiceTest.stockTransferRepository
                this.tenantFilterService = this@StockTransferServiceTest.tenantFilterService
                this.organizationIdHolder = this@StockTransferServiceTest.organizationIdHolder
            }
        whenever(organizationIdHolder.organizationId).thenReturn(orgId)
    }

    @Test
    fun `create sets PENDING status`() {
        // Arrange
        val fromStore = UUID.randomUUID()
        val toStore = UUID.randomUUID()
        whenever(stockTransferRepository.isPersistent(any<StockTransferEntity>())).thenReturn(true)

        // Act
        val result = service.create(fromStore, toStore, """[{"productId":"abc","quantity":10}]""", "転送テスト")

        // Assert
        assertNotNull(result)
        assertEquals(fromStore, result.fromStoreId)
        assertEquals(toStore, result.toStoreId)
        assertEquals("PENDING", result.status)
        verify(stockTransferRepository).persist(any<StockTransferEntity>())
    }

    @Test
    fun `updateStatus changes status`() {
        // Arrange
        val transferId = UUID.randomUUID()
        val entity =
            StockTransferEntity().apply {
                id = transferId
                organizationId = orgId
                fromStoreId = UUID.randomUUID()
                toStoreId = UUID.randomUUID()
                items = "[]"
                status = "PENDING"
            }
        whenever(stockTransferRepository.findById(transferId)).thenReturn(entity)

        // Act
        val result = service.updateStatus(transferId, "IN_TRANSIT")

        // Assert
        assertNotNull(result)
        assertEquals("IN_TRANSIT", result?.status)
    }
}
