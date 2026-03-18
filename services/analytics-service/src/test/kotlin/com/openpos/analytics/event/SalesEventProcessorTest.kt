package com.openpos.analytics.event

import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID

@QuarkusTest
class SalesEventProcessorTest {
    @Inject
    lateinit var salesEventProcessor: SalesEventProcessor

    @InjectMock
    lateinit var dailySalesRepository: DailySalesRepository

    @InjectMock
    lateinit var productSalesRepository: ProductSalesRepository

    @InjectMock
    lateinit var hourlySalesRepository: HourlySalesRepository

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId1 = UUID.randomUUID()
    private val productId2 = UUID.randomUUID()

    @Nested
    inner class ProcessSaleCompleted {
        @Test
        fun `updates daily sales for new date`() {
            // Arrange
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)
            doNothing().whenever(dailySalesRepository).persist(any<DailySalesEntity>())
            doNothing().whenever(productSalesRepository).persist(any<ProductSalesEntity>())
            doNothing().whenever(hourlySalesRepository).persist(any<HourlySalesEntity>())

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-001",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items =
                        listOf(
                            SaleItemPayload(productId1.toString(), 2, 10000, 20000),
                            SaleItemPayload(productId2.toString(), 1, 5000, 5000),
                        ),
                    totalAmount = 25000,
                    transactedAt = "2026-03-06T10:00:00Z",
                )

            // Act
            salesEventProcessor.processSaleCompleted(orgId, payload)

            // Assert
            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 25000L && transactionCount == 1
                },
            )
            verify(productSalesRepository).persist(
                argThat<ProductSalesEntity> {
                    productId == productId1 && quantitySold == 2 && totalAmount == 20000L
                },
            )
            verify(productSalesRepository).persist(
                argThat<ProductSalesEntity> {
                    productId == productId2 && quantitySold == 1 && totalAmount == 5000L
                },
            )
            verify(hourlySalesRepository).persist(
                argThat<HourlySalesEntity> {
                    totalSales == 25000L && transactionCount == 1
                },
            )
        }

        @Test
        fun `increments existing daily sales`() {
            // Arrange
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorTest.storeId
                    date = LocalDate.of(2026, 3, 6)
                    grossAmount = 10000
                    transactionCount = 1
                    netAmount = 10000
                }
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)
            doNothing().whenever(dailySalesRepository).persist(any<DailySalesEntity>())
            doNothing().whenever(productSalesRepository).persist(any<ProductSalesEntity>())
            doNothing().whenever(hourlySalesRepository).persist(any<HourlySalesEntity>())

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-002",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 10000, 10000)),
                    totalAmount = 10000,
                    transactedAt = "2026-03-06T14:00:00Z",
                )

            // Act
            salesEventProcessor.processSaleCompleted(orgId, payload)

            // Assert
            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 20000L && transactionCount == 2
                },
            )
        }
    }

    @Nested
    inner class ProcessSaleVoided {
        @Test
        fun `decrements daily sales using original transaction date`() {
            // Arrange
            val originalDate = LocalDate.of(2026, 3, 6)
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorTest.storeId
                    date = originalDate
                    grossAmount = 25000
                    transactionCount = 2
                    netAmount = 25000
                }
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)

            val existingProduct =
                ProductSalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorTest.storeId
                    productId = productId1
                    date = originalDate
                    quantitySold = 5
                    totalAmount = 50000
                    transactionCount = 3
                }
            whenever(productSalesRepository.findByStoreProductAndDate(any(), eq(productId1), any())).thenReturn(existingProduct)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), eq(productId2), any())).thenReturn(null)

            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)
            doNothing().whenever(dailySalesRepository).persist(any<DailySalesEntity>())
            doNothing().whenever(productSalesRepository).persist(any<ProductSalesEntity>())

            val payload =
                SaleVoidedPayload(
                    originalTransactionId = "txn-001",
                    voidTransactionId = "void-001",
                    storeId = storeId.toString(),
                    items =
                        listOf(
                            SaleItemPayload(productId1.toString(), 2, 10000, 20000),
                        ),
                    originalTransactedAt = "2026-03-06T10:00:00Z",
                )

            // Act
            salesEventProcessor.processSaleVoided(orgId, payload)

            // Assert — rollback targets original date, not today
            verify(dailySalesRepository).findByStoreAndDate(storeId, originalDate)
            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 5000L
                },
            )
            verify(productSalesRepository).persist(
                argThat<ProductSalesEntity> {
                    quantitySold == 3 && totalAmount == 30000L
                },
            )
        }

        @Test
        fun `does not go below zero on rollback`() {
            // Arrange
            val originalDate = LocalDate.of(2026, 3, 5)
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorTest.storeId
                    date = originalDate
                    grossAmount = 5000
                    transactionCount = 1
                    netAmount = 5000
                }
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)
            doNothing().whenever(dailySalesRepository).persist(any<DailySalesEntity>())

            val payload =
                SaleVoidedPayload(
                    originalTransactionId = "txn-001",
                    voidTransactionId = "void-001",
                    storeId = storeId.toString(),
                    items =
                        listOf(
                            SaleItemPayload(productId1.toString(), 10, 10000, 100000),
                        ),
                    originalTransactedAt = "2026-03-05T15:30:00Z",
                )

            // Act
            salesEventProcessor.processSaleVoided(orgId, payload)

            // Assert
            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 0L
                },
            )
        }
    }
}
