package com.openpos.analytics.event

import com.openpos.analytics.entity.DailySalesEntity
import com.openpos.analytics.entity.HourlySalesEntity
import com.openpos.analytics.entity.ProductSalesEntity
import com.openpos.analytics.repository.DailySalesRepository
import com.openpos.analytics.repository.HourlySalesRepository
import com.openpos.analytics.repository.ProductSalesRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * SalesEventProcessor の純粋ユニットテスト。
 * CDI プロキシを回避して JaCoCo カバレッジを確保する。
 */
class SalesEventProcessorUnitTest {
    private lateinit var processor: SalesEventProcessor
    private lateinit var dailySalesRepository: DailySalesRepository
    private lateinit var productSalesRepository: ProductSalesRepository
    private lateinit var hourlySalesRepository: HourlySalesRepository

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val productId1 = UUID.randomUUID()
    private val productId2 = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        dailySalesRepository = mock()
        productSalesRepository = mock()
        hourlySalesRepository = mock()

        processor = SalesEventProcessor()
        processor.dailySalesRepository = dailySalesRepository
        processor.productSalesRepository = productSalesRepository
        processor.hourlySalesRepository = hourlySalesRepository
        processor.timezoneName = "UTC"

        doNothing().whenever(dailySalesRepository).persist(any<DailySalesEntity>())
        doNothing().whenever(productSalesRepository).persist(any<ProductSalesEntity>())
        doNothing().whenever(hourlySalesRepository).persist(any<HourlySalesEntity>())
    }

    @Nested
    inner class ProcessSaleCompleted {
        @Test
        fun `creates new daily, product, and hourly sales when none exist`() {
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-001",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items =
                        listOf(
                            SaleItemPayload(productId1.toString(), 2, 10000, 20000, "Product A"),
                            SaleItemPayload(productId2.toString(), 1, 5000, 5000),
                        ),
                    totalAmount = 25000,
                    transactedAt = "2026-03-06T10:00:00Z",
                    taxTotal = 2500,
                    discountTotal = 500,
                    payments =
                        listOf(
                            SalePaymentPayload("CASH", 15000),
                            SalePaymentPayload("CREDIT_CARD", 10000),
                        ),
                )

            processor.processSaleCompleted(orgId, payload)

            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 25000L && taxAmount == 2500L && discountAmount == 500L &&
                        transactionCount == 1 && cashAmount == 15000L && cardAmount == 10000L
                },
            )
            verify(hourlySalesRepository).persist(
                argThat<HourlySalesEntity> {
                    totalSales == 25000L && transactionCount == 1 && hour == 10
                },
            )
        }

        @Test
        fun `increments existing records`() {
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    grossAmount = 10000
                    taxAmount = 1000
                    discountAmount = 0
                    transactionCount = 1
                    netAmount = 9000
                }
            val existingProduct =
                ProductSalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    productId = productId1
                    quantitySold = 3
                    totalAmount = 30000
                    transactionCount = 2
                }
            val existingHourly =
                HourlySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    totalSales = 10000
                    transactionCount = 1
                    hour = 14
                }

            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(existingProduct)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(existingHourly)

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-002",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 10000, 10000)),
                    totalAmount = 10000,
                    transactedAt = "2026-03-06T14:30:00Z",
                )

            processor.processSaleCompleted(orgId, payload)

            assertEquals(20000, existingDaily.grossAmount)
            assertEquals(2, existingDaily.transactionCount)
            assertEquals(4, existingProduct.quantitySold)
            assertEquals(40000, existingProduct.totalAmount)
            assertEquals(20000, existingHourly.totalSales)
            assertEquals(2, existingHourly.transactionCount)
        }

        @Test
        fun `handles QR_CODE payment method`() {
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-003",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 5000, 5000)),
                    totalAmount = 5000,
                    transactedAt = "2026-03-06T12:00:00Z",
                    payments = listOf(SalePaymentPayload("QR_CODE", 5000)),
                )

            processor.processSaleCompleted(orgId, payload)

            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    qrAmount == 5000L
                },
            )
        }

        @Test
        fun `handles null product name with fallback`() {
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-004",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 1000, 1000, null)),
                    totalAmount = 1000,
                    transactedAt = "2026-03-06T09:00:00Z",
                )

            processor.processSaleCompleted(orgId, payload)

            verify(productSalesRepository).persist(
                argThat<ProductSalesEntity> {
                    productName.startsWith("Product-")
                },
            )
        }

        @Test
        fun `handles blank product name with fallback`() {
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleCompletedPayload(
                    transactionId = "txn-005",
                    storeId = storeId.toString(),
                    terminalId = "terminal-1",
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 1000, 1000, "  ")),
                    totalAmount = 1000,
                    transactedAt = "2026-03-06T09:00:00Z",
                )

            processor.processSaleCompleted(orgId, payload)

            verify(productSalesRepository).persist(
                argThat<ProductSalesEntity> {
                    productName.startsWith("Product-")
                },
            )
        }
    }

    @Nested
    inner class ProcessSaleVoided {
        @Test
        fun `rolls back daily, product, and hourly sales`() {
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    grossAmount = 25000
                    taxAmount = 0
                    discountAmount = 0
                    transactionCount = 2
                    netAmount = 25000
                }
            val existingProduct =
                ProductSalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    productId = productId1
                    quantitySold = 5
                    totalAmount = 50000
                    transactionCount = 3
                }
            val existingHourly =
                HourlySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    totalSales = 25000
                    transactionCount = 2
                    hour = 10
                }

            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(existingProduct)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(existingHourly)

            val payload =
                SaleVoidedPayload(
                    originalTransactionId = "txn-001",
                    voidTransactionId = "void-001",
                    storeId = storeId.toString(),
                    items = listOf(SaleItemPayload(productId1.toString(), 2, 10000, 20000)),
                    originalTransactedAt = "2026-03-06T10:00:00Z",
                )

            processor.processSaleVoided(orgId, payload)

            assertEquals(5000, existingDaily.grossAmount)
            assertEquals(1, existingDaily.transactionCount)
            assertEquals(3, existingProduct.quantitySold)
            assertEquals(30000, existingProduct.totalAmount)
            assertEquals(5000, existingHourly.totalSales)
        }

        @Test
        fun `does not go below zero on rollback`() {
            val existingDaily =
                DailySalesEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    this.storeId = this@SalesEventProcessorUnitTest.storeId
                    grossAmount = 5000
                    taxAmount = 0
                    discountAmount = 0
                    transactionCount = 1
                    netAmount = 5000
                }
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(existingDaily)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleVoidedPayload(
                    originalTransactionId = "txn-001",
                    voidTransactionId = "void-001",
                    storeId = storeId.toString(),
                    items = listOf(SaleItemPayload(productId1.toString(), 10, 10000, 100000)),
                    originalTransactedAt = "2026-03-05T15:30:00Z",
                )

            processor.processSaleVoided(orgId, payload)

            assertEquals(0, existingDaily.grossAmount)
            assertEquals(0, existingDaily.transactionCount)
        }

        @Test
        fun `creates new daily when none exists for rollback`() {
            whenever(dailySalesRepository.findByStoreAndDate(any(), any())).thenReturn(null)
            whenever(productSalesRepository.findByStoreProductAndDate(any(), any(), any())).thenReturn(null)
            whenever(hourlySalesRepository.findByStoreAndDateAndHour(any(), any(), any())).thenReturn(null)

            val payload =
                SaleVoidedPayload(
                    originalTransactionId = "txn-001",
                    voidTransactionId = "void-001",
                    storeId = storeId.toString(),
                    items = listOf(SaleItemPayload(productId1.toString(), 1, 1000, 1000)),
                    originalTransactedAt = "2026-03-06T10:00:00Z",
                )

            processor.processSaleVoided(orgId, payload)

            verify(dailySalesRepository).persist(
                argThat<DailySalesEntity> {
                    grossAmount == 0L && transactionCount == 0
                },
            )
        }
    }
}
