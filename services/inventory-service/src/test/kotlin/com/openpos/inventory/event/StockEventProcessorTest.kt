package com.openpos.inventory.event

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.entity.StockEntity
import com.openpos.inventory.service.StockService
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class StockEventProcessorTest {
    @Inject
    lateinit var stockEventProcessor: StockEventProcessor

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var stockService: StockService

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId1 = UUID.randomUUID()
    private val productId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.clear()
    }

    @Test
    fun `processSaleCompleted decreases stock for each item`() {
        // Arrange
        val payload =
            SaleCompletedPayload(
                transactionId = "txn-001",
                storeId = storeId.toString(),
                terminalId = "terminal-1",
                items =
                    listOf(
                        SaleItemPayload(productId = productId1.toString(), quantity = 2, unitPrice = 10000, subtotal = 20000),
                        SaleItemPayload(productId = productId2.toString(), quantity = 1, unitPrice = 5000, subtotal = 5000),
                    ),
                totalAmount = 25000,
                transactedAt = "2026-03-06T10:00:00Z",
            )
        whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(StockEntity())

        // Act
        stockEventProcessor.processSaleCompleted(orgId, payload)

        // Assert
        assertEquals(2, payload.items.size)
        verify(stockService, times(2)).adjustStock(any(), any(), any(), any(), any(), anyOrNull())
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(-2),
            movementType = eq("SALE"),
            referenceId = eq("txn-001"),
            note = eq(null),
        )
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId2),
            quantityChange = eq(-1),
            movementType = eq("SALE"),
            referenceId = eq("txn-001"),
            note = eq(null),
        )
        assertNull(organizationIdHolder.organizationId)
    }

    @Test
    fun `processSaleVoided increases stock for each item`() {
        // Arrange
        val payload =
            SaleVoidedPayload(
                originalTransactionId = "txn-001",
                voidTransactionId = "void-001",
                storeId = storeId.toString(),
                items =
                    listOf(
                        SaleItemPayload(productId = productId1.toString(), quantity = 2, unitPrice = 10000, subtotal = 20000),
                        SaleItemPayload(productId = productId2.toString(), quantity = 3, unitPrice = 5000, subtotal = 15000),
                    ),
            )
        whenever(stockService.adjustStock(any(), any(), any(), any(), any(), any())).thenReturn(StockEntity())

        // Act
        stockEventProcessor.processSaleVoided(orgId, payload)

        // Assert
        assertEquals(2, payload.items.size)
        verify(stockService, times(2)).adjustStock(any(), any(), any(), any(), any(), anyOrNull())
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId1),
            quantityChange = eq(2),
            movementType = eq("RETURN"),
            referenceId = eq("void-001"),
            note = eq("Void of transaction txn-001"),
        )
        verify(stockService).adjustStock(
            storeId = eq(storeId),
            productId = eq(productId2),
            quantityChange = eq(3),
            movementType = eq("RETURN"),
            referenceId = eq("void-001"),
            note = eq("Void of transaction txn-001"),
        )
        assertNull(organizationIdHolder.organizationId)
    }
}
