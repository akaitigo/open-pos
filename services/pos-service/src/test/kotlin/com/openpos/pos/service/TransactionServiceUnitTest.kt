package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.grpc.BusinessPreconditionException
import com.openpos.pos.grpc.InvalidInputException
import com.openpos.pos.grpc.ProductServiceClient
import com.openpos.pos.grpc.ProductSnapshot
import com.openpos.pos.grpc.ResourceNotFoundException
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionDiscountRepository
import com.openpos.pos.repository.TransactionItemRepository
import com.openpos.pos.repository.TransactionRepository
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import io.quarkus.panache.common.Page
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

/**
 * TransactionService の純粋ユニットテスト。
 * CDI プロキシを回避して JaCoCo カバレッジを確保する。
 */
class TransactionServiceUnitTest {
    private lateinit var service: TransactionService
    private lateinit var transactionRepository: TransactionRepository
    private lateinit var itemRepository: TransactionItemRepository
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var discountRepository: TransactionDiscountRepository
    private lateinit var taxSummaryRepository: TaxSummaryRepository
    private lateinit var taxCalculationService: TaxCalculationService
    private lateinit var productServiceClient: ProductServiceClient
    private lateinit var eventPublisher: EventPublisher
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()
    private val storeId = UUID.randomUUID()
    private val terminalId = UUID.randomUUID()
    private val staffId = UUID.randomUUID()
    private val productId = UUID.randomUUID()

    private val standardProduct =
        ProductSnapshot(
            name = "Test Product",
            price = 10000,
            taxRateName = "Standard 10%",
            taxRate = "0.10",
            isReduced = false,
        )

    @BeforeEach
    fun setUp() {
        transactionRepository = mock()
        itemRepository = mock()
        paymentRepository = mock()
        discountRepository = mock()
        taxSummaryRepository = mock()
        taxCalculationService = TaxCalculationService()
        productServiceClient = mock()
        eventPublisher = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = TransactionService()
        service.transactionRepository = transactionRepository
        service.itemRepository = itemRepository
        service.paymentRepository = paymentRepository
        service.discountRepository = discountRepository
        service.taxSummaryRepository = taxSummaryRepository
        service.taxCalculationService = taxCalculationService
        service.productServiceClient = productServiceClient
        service.eventPublisher = eventPublisher
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder
        service.meterRegistry = SimpleMeterRegistry()

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(transactionRepository).persist(any<TransactionEntity>())
        doNothing().whenever(itemRepository).persist(any<TransactionItemEntity>())
        doNothing().whenever(paymentRepository).persist(any<PaymentEntity>())
        doNothing().whenever(discountRepository).persist(any<TransactionDiscountEntity>())
        doNothing().whenever(taxSummaryRepository).persist(any<TaxSummaryEntity>())
    }

    @Nested
    inner class CreateTransaction {
        @Test
        fun `creates DRAFT transaction`() {
            val result = service.createTransaction(storeId, terminalId, staffId, "SALE", null)

            assertEquals("DRAFT", result.status)
            assertEquals("SALE", result.type)
            assertEquals(orgId, result.organizationId)
            assertNotNull(result.transactionNumber)
            verify(transactionRepository).persist(any<TransactionEntity>())
        }

        @Test
        fun `empty type defaults to SALE`() {
            val result = service.createTransaction(storeId, terminalId, staffId, "", null)

            assertEquals("SALE", result.type)
        }

        @Test
        fun `returns existing transaction for same clientId`() {
            val clientId = "test-client-id"
            val existing = createTransactionEntity().apply { this.clientId = clientId }
            whenever(transactionRepository.findByClientId(clientId, orgId)).thenReturn(existing)

            val result = service.createTransaction(storeId, terminalId, staffId, "SALE", clientId)

            assertEquals(existing.id, result.id)
        }

        @Test
        fun `blank clientId creates new transaction`() {
            val result = service.createTransaction(storeId, terminalId, staffId, "SALE", "  ")

            assertEquals("DRAFT", result.status)
            assertEquals("SALE", result.type)
            verify(transactionRepository).persist(any<TransactionEntity>())
        }
    }

    @Nested
    inner class AddItem {
        @Test
        fun `adds new item to transaction`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(productServiceClient.getProductSnapshot(productId, orgId)).thenReturn(standardProduct)
            whenever(itemRepository.findByTransactionAndProduct(tx.id, productId)).thenReturn(null)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            val result = service.addItem(tx.id, productId, 2)

            verify(itemRepository).persist(any<TransactionItemEntity>())
            assertNotNull(result)
        }

        @Test
        fun `merges quantity for existing item`() {
            val tx = createTransactionEntity()
            val existingItem =
                TransactionItemEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.transactionId = tx.id
                    this.productId = this@TransactionServiceUnitTest.productId
                    this.productName = "Test"
                    this.unitPrice = 10000
                    this.quantity = 2
                    this.taxRateName = "Standard 10%"
                    this.taxRate = "0.10"
                    this.isReducedTax = false
                    this.subtotal = 20000
                    this.taxAmount = 2000
                    this.total = 22000
                }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(productServiceClient.getProductSnapshot(productId, orgId)).thenReturn(standardProduct)
            whenever(itemRepository.findByTransactionAndProduct(tx.id, productId)).thenReturn(existingItem)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(existingItem))
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            val result = service.addItem(tx.id, productId, 3)

            assertEquals(5, existingItem.quantity)
            verify(itemRepository).persist(existingItem)
        }

        @Test
        fun `throws on zero quantity`() {
            assertThrows(InvalidInputException::class.java) {
                service.addItem(UUID.randomUUID(), productId, 0)
            }
        }

        @Test
        fun `throws when transaction not found`() {
            whenever(transactionRepository.findById(any<UUID>())).thenReturn(null)

            assertThrows(ResourceNotFoundException::class.java) {
                service.addItem(UUID.randomUUID(), productId, 1)
            }
        }

        @Test
        fun `uses provided productSnapshot`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findByTransactionAndProduct(tx.id, productId)).thenReturn(null)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            val result = service.addItem(tx.id, productId, 1, standardProduct)

            assertNotNull(result)
            assertEquals("DRAFT", result.status)
            verify(itemRepository).persist(any<TransactionItemEntity>())
        }
    }

    @Nested
    inner class UpdateItem {
        @Test
        fun `updates item quantity`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id)
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findById(item.id)).thenReturn(item)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            service.updateItem(tx.id, item.id, 5)

            assertEquals(5, item.quantity)
        }

        @Test
        fun `throws on zero quantity`() {
            assertThrows(InvalidInputException::class.java) {
                service.updateItem(UUID.randomUUID(), UUID.randomUUID(), 0)
            }
        }

        @Test
        fun `throws when item not found`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findById(any<UUID>())).thenReturn(null)

            assertThrows(ResourceNotFoundException::class.java) {
                service.updateItem(tx.id, UUID.randomUUID(), 5)
            }
        }

        @Test
        fun `throws when item belongs to different transaction`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(UUID.randomUUID()) // different transaction
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findById(item.id)).thenReturn(item)

            assertThrows(BusinessPreconditionException::class.java) {
                service.updateItem(tx.id, item.id, 5)
            }
        }
    }

    @Nested
    inner class RemoveItem {
        @Test
        fun `removes item from transaction`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id)
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findById(item.id)).thenReturn(item)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            doNothing().whenever(itemRepository).delete(any<TransactionItemEntity>())

            val result = service.removeItem(tx.id, item.id)

            assertEquals("DRAFT", result.status)
            assertEquals(0, result.subtotal)
            verify(itemRepository).delete(item)
        }

        @Test
        fun `throws when item belongs to different transaction`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(UUID.randomUUID())
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findById(item.id)).thenReturn(item)

            assertThrows(BusinessPreconditionException::class.java) {
                service.removeItem(tx.id, item.id)
            }
        }
    }

    @Nested
    inner class ApplyDiscount {
        @Test
        fun `applies percentage discount`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            val result = service.applyDiscount(tx.id, null, "10% Off", "PERCENTAGE", "10", 1000, null)

            assertEquals("DRAFT", result.status)
            verify(discountRepository).persist(any<TransactionDiscountEntity>())
        }

        @Test
        fun `throws on negative discount amount`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Bad", "PERCENTAGE", "10", -100, null)
            }
        }

        @Test
        fun `throws on unknown discount type`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Bad", "UNKNOWN", "10", 100, null)
            }
        }

        @Test
        fun `throws on duplicate discountId`() {
            val tx = createTransactionEntity()
            val discountId = UUID.randomUUID()
            val existing =
                TransactionDiscountEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.transactionId = tx.id
                    this.discountId = discountId
                    this.name = "Existing"
                    this.discountType = "PERCENTAGE"
                    this.value = "5"
                    this.amount = 500
                }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(listOf(existing))

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, discountId, "Dup", "PERCENTAGE", "10", 1000, null)
            }
        }

        @Test
        fun `applies fixed amount discount to whole transaction`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id).apply { this.subtotal = 50000 }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))

            val result = service.applyDiscount(tx.id, null, "500 Off", "FIXED_AMOUNT", "5000", 5000, null)

            assertEquals("DRAFT", result.status)
            verify(discountRepository).persist(any<TransactionDiscountEntity>())
        }

        @Test
        fun `throws when fixed amount exceeds subtotal`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id).apply { this.subtotal = 1000 }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Too Much", "FIXED_AMOUNT", "5000", 5000, null)
            }
        }

        @Test
        fun `applies fixed amount discount to specific item`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id).apply { this.subtotal = 50000 }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findById(item.id)).thenReturn(item)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))

            val result = service.applyDiscount(tx.id, null, "Item Off", "FIXED_AMOUNT", "1000", 1000, item.id)

            assertEquals("DRAFT", result.status)
            verify(discountRepository).persist(any<TransactionDiscountEntity>())
        }

        @Test
        fun `throws when fixed amount item not found`() {
            val tx = createTransactionEntity()
            val fakeItemId = UUID.randomUUID()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findById(fakeItemId)).thenReturn(null)

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Item Off", "FIXED_AMOUNT", "1000", 1000, fakeItemId)
            }
        }

        @Test
        fun `throws when fixed amount item belongs to different transaction`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(UUID.randomUUID()).apply { this.subtotal = 50000 } // different transaction
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(itemRepository.findById(item.id)).thenReturn(item)

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Item Off", "FIXED_AMOUNT", "1000", 1000, item.id)
            }
        }

        @Test
        fun `throws on invalid percentage value`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Bad", "PERCENTAGE", "not-a-number", 0, null)
            }
        }

        @Test
        fun `throws when percentage out of range`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            assertThrows(IllegalArgumentException::class.java) {
                service.applyDiscount(tx.id, null, "Over", "PERCENTAGE", "101", 0, null)
            }
        }
    }

    @Nested
    inner class FinalizeTransaction {
        @Test
        fun `finalizes transaction with CASH payment`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id)
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(paymentRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            doNothing().whenever(taxSummaryRepository).deleteByTransactionId(tx.id)

            val payments = listOf(PaymentInput("CASH", 11000, 15000, null))
            val result = service.finalizeTransaction(tx.id, payments)

            assertEquals("COMPLETED", result.status)
            assertNotNull(result.completedAt)
            assertNotNull(result.contentHash)
            verify(eventPublisher).publish(eq("sale.completed"), eq(orgId), any())
        }

        @Test
        fun `throws when transaction not found`() {
            whenever(transactionRepository.findById(any<UUID>())).thenReturn(null)

            assertThrows(ResourceNotFoundException::class.java) {
                service.finalizeTransaction(UUID.randomUUID(), emptyList())
            }
        }

        @Test
        fun `throws when transaction not DRAFT`() {
            val tx = createTransactionEntity().apply { this.status = "COMPLETED" }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)

            assertThrows(BusinessPreconditionException::class.java) {
                service.finalizeTransaction(tx.id, emptyList())
            }
        }

        @Test
        fun `throws when no items`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            assertThrows(BusinessPreconditionException::class.java) {
                service.finalizeTransaction(tx.id, listOf(PaymentInput("CASH", 0, 0, null)))
            }
        }

        @Test
        fun `throws when payment insufficient`() {
            val tx = createTransactionEntity()
            val item = createItemEntity(tx.id)
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(listOf(item))
            whenever(discountRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            assertThrows(BusinessPreconditionException::class.java) {
                service.finalizeTransaction(tx.id, listOf(PaymentInput("CASH", 100, 100, null)))
            }
        }
    }

    @Nested
    inner class VoidTransaction {
        @Test
        fun `voids completed transaction`() {
            val tx =
                createTransactionEntity().apply {
                    this.status = "COMPLETED"
                    this.completedAt = Instant.now()
                }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)
            whenever(itemRepository.findByTransactionId(tx.id)).thenReturn(emptyList())

            val result = service.voidTransaction(tx.id, "Customer request")

            assertEquals("VOIDED", result.status)
            verify(eventPublisher).publish(eq("sale.voided"), eq(orgId), any())
        }

        @Test
        fun `throws when transaction not found`() {
            whenever(transactionRepository.findById(any<UUID>())).thenReturn(null)

            assertThrows(ResourceNotFoundException::class.java) {
                service.voidTransaction(UUID.randomUUID(), "reason")
            }
        }

        @Test
        fun `throws when not COMPLETED`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)

            assertThrows(BusinessPreconditionException::class.java) {
                service.voidTransaction(tx.id, "reason")
            }
        }

        @Test
        fun `throws when reason is blank`() {
            val tx = createTransactionEntity().apply { this.status = "COMPLETED" }
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)

            assertThrows(InvalidInputException::class.java) {
                service.voidTransaction(tx.id, "  ")
            }
        }
    }

    @Nested
    inner class QueryMethods {
        @Test
        fun `getTransaction returns entity`() {
            val tx = createTransactionEntity()
            whenever(transactionRepository.findById(tx.id)).thenReturn(tx)

            val result = service.getTransaction(tx.id)

            assertEquals(tx.id, result.id)
        }

        @Test
        fun `getTransaction throws when not found`() {
            whenever(transactionRepository.findById(any<UUID>())).thenReturn(null)

            assertThrows(ResourceNotFoundException::class.java) {
                service.getTransaction(UUID.randomUUID())
            }
        }

        @Test
        fun `getTransactionItems delegates to repository`() {
            val txId = UUID.randomUUID()
            whenever(itemRepository.findByTransactionId(txId)).thenReturn(emptyList())

            val result = service.getTransactionItems(txId)

            assertEquals(0, result.size)
        }

        @Test
        fun `getTransactionPayments delegates to repository`() {
            val txId = UUID.randomUUID()
            whenever(paymentRepository.findByTransactionId(txId)).thenReturn(emptyList())

            val result = service.getTransactionPayments(txId)

            assertEquals(0, result.size)
        }

        @Test
        fun `getTransactionDiscounts delegates to repository`() {
            val txId = UUID.randomUUID()
            whenever(discountRepository.findByTransactionId(txId)).thenReturn(emptyList())

            val result = service.getTransactionDiscounts(txId)

            assertEquals(0, result.size)
        }

        @Test
        fun `getTransactionTaxSummaries delegates to repository`() {
            val txId = UUID.randomUUID()
            whenever(taxSummaryRepository.findByTransactionId(txId)).thenReturn(emptyList())

            val result = service.getTransactionTaxSummaries(txId)

            assertEquals(0, result.size)
        }

        @Test
        fun `listTransactions returns paginated results`() {
            whenever(transactionRepository.listByFilters(any(), any(), any(), any(), any(), any<Page>())).thenReturn(emptyList())
            whenever(transactionRepository.countByFilters(any(), any(), any(), any(), any())).thenReturn(0L)

            val (result, total) = service.listTransactions(null, null, null, null, null, 0, 20)

            assertEquals(0, result.size)
            assertEquals(0L, total)
        }

        @Test
        fun `batchLoadRelations returns empty maps for empty input`() {
            val result = service.batchLoadRelations(emptyList())

            assertTrue(result.items.isEmpty())
            assertTrue(result.payments.isEmpty())
            assertTrue(result.discounts.isEmpty())
            assertTrue(result.taxSummaries.isEmpty())
        }

        @Test
        fun `batchLoadRelations loads relations for transaction ids`() {
            val txId = UUID.randomUUID()
            whenever(itemRepository.findByTransactionIds(listOf(txId))).thenReturn(emptyList())
            whenever(paymentRepository.findByTransactionIds(listOf(txId))).thenReturn(emptyList())
            whenever(discountRepository.findByTransactionIds(listOf(txId))).thenReturn(emptyList())
            whenever(taxSummaryRepository.findByTransactionIds(listOf(txId))).thenReturn(emptyList())

            val result = service.batchLoadRelations(listOf(txId))

            assertNotNull(result)
        }

        @Test
        fun `fetchProductSnapshot delegates to client`() {
            whenever(productServiceClient.getProductSnapshot(productId, orgId)).thenReturn(standardProduct)

            val result = service.fetchProductSnapshot(productId, orgId)

            assertEquals("Test Product", result.name)
        }
    }

    private fun createTransactionEntity(): TransactionEntity =
        TransactionEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.storeId = this@TransactionServiceUnitTest.storeId
            this.terminalId = this@TransactionServiceUnitTest.terminalId
            this.staffId = this@TransactionServiceUnitTest.staffId
            this.transactionNumber = "T-2026-03-23-test"
            this.type = "SALE"
            this.status = "DRAFT"
        }

    private fun createItemEntity(transactionId: UUID): TransactionItemEntity =
        TransactionItemEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.transactionId = transactionId
            this.productId = this@TransactionServiceUnitTest.productId
            this.productName = "Test Product"
            this.unitPrice = 10000
            this.quantity = 1
            this.taxRateName = "Standard 10%"
            this.taxRate = "0.10"
            this.isReducedTax = false
            this.subtotal = 10000
            this.taxAmount = 1000
            this.total = 11000
        }
}
