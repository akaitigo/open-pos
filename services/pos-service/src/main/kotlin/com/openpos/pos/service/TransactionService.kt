package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.event.SaleCompletedEventDto
import com.openpos.pos.event.SaleItemDto
import com.openpos.pos.event.SaleVoidedEventDto
import com.openpos.pos.grpc.ProductServiceClient
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionDiscountRepository
import com.openpos.pos.repository.TransactionItemRepository
import com.openpos.pos.repository.TransactionRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@ApplicationScoped
class TransactionService {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var itemRepository: TransactionItemRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var discountRepository: TransactionDiscountRepository

    @Inject
    lateinit var taxSummaryRepository: TaxSummaryRepository

    @Inject
    lateinit var taxCalculationService: TaxCalculationService

    @Inject
    lateinit var productServiceClient: ProductServiceClient

    @Inject
    lateinit var eventPublisher: EventPublisher

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    // === Create ===

    @Transactional
    fun createTransaction(
        storeId: UUID,
        terminalId: UUID,
        staffId: UUID,
        type: String,
        clientId: String?,
    ): TransactionEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        if (!clientId.isNullOrBlank()) {
            val existing = transactionRepository.findByClientId(clientId)
            if (existing != null) return existing
        }

        val entity =
            TransactionEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalId = terminalId
                this.staffId = staffId
                this.transactionNumber = generateTransactionNumber()
                this.type = type.ifBlank { "SALE" }
                this.status = "DRAFT"
                this.clientId = clientId?.ifBlank { null }
            }
        transactionRepository.persist(entity)
        return entity
    }

    // === Item Operations ===

    @Transactional
    fun addItem(
        transactionId: UUID,
        productId: UUID,
        quantity: Int,
    ): TransactionEntity {
        require(quantity > 0) { "quantity must be positive" }
        val tx = getWritableTransaction(transactionId)
        val orgId = tx.organizationId

        val product = productServiceClient.getProductSnapshot(productId, orgId)

        val existingItem = itemRepository.findByTransactionAndProduct(transactionId, productId)
        if (existingItem != null) {
            existingItem.quantity += quantity
            val taxResult =
                taxCalculationService.calculateItemTax(
                    existingItem.unitPrice,
                    existingItem.quantity,
                    existingItem.taxRate,
                )
            existingItem.subtotal = taxResult.subtotal
            existingItem.taxAmount = taxResult.taxAmount
            existingItem.total = taxResult.total
            itemRepository.persist(existingItem)
        } else {
            val taxResult = taxCalculationService.calculateItemTax(product.price, quantity, product.taxRate)
            val item =
                TransactionItemEntity().apply {
                    this.organizationId = orgId
                    this.transactionId = transactionId
                    this.productId = productId
                    this.productName = product.name
                    this.unitPrice = product.price
                    this.quantity = quantity
                    this.taxRateName = product.taxRateName
                    this.taxRate = product.taxRate
                    this.isReducedTax = product.isReduced
                    this.subtotal = taxResult.subtotal
                    this.taxAmount = taxResult.taxAmount
                    this.total = taxResult.total
                }
            itemRepository.persist(item)
        }

        recalculateTransactionTotals(tx)
        return tx
    }

    @Transactional
    fun updateItem(
        transactionId: UUID,
        itemId: UUID,
        quantity: Int,
    ): TransactionEntity {
        require(quantity > 0) { "quantity must be positive, use removeItem for deletion" }
        val tx = getWritableTransaction(transactionId)

        val item =
            itemRepository.findById(itemId)
                ?: throw IllegalArgumentException("Item not found: $itemId")
        require(item.transactionId == transactionId) { "Item does not belong to this transaction" }

        item.quantity = quantity
        val taxResult = taxCalculationService.calculateItemTax(item.unitPrice, quantity, item.taxRate)
        item.subtotal = taxResult.subtotal
        item.taxAmount = taxResult.taxAmount
        item.total = taxResult.total
        itemRepository.persist(item)

        recalculateTransactionTotals(tx)
        return tx
    }

    @Transactional
    fun removeItem(
        transactionId: UUID,
        itemId: UUID,
    ): TransactionEntity {
        val tx = getWritableTransaction(transactionId)

        val item =
            itemRepository.findById(itemId)
                ?: throw IllegalArgumentException("Item not found: $itemId")
        require(item.transactionId == transactionId) { "Item does not belong to this transaction" }

        itemRepository.delete(item)
        recalculateTransactionTotals(tx)
        return tx
    }

    // === Discount ===

    @Transactional
    fun applyDiscount(
        transactionId: UUID,
        discountId: UUID?,
        name: String,
        discountType: String,
        value: String,
        amount: Long,
        transactionItemId: UUID?,
    ): TransactionEntity {
        val tx = getWritableTransaction(transactionId)

        val discount =
            TransactionDiscountEntity().apply {
                this.organizationId = tx.organizationId
                this.transactionId = transactionId
                this.discountId = discountId
                this.name = name
                this.discountType = discountType
                this.value = value
                this.amount = amount
                this.transactionItemId = transactionItemId
            }
        discountRepository.persist(discount)

        recalculateTransactionTotals(tx)
        return tx
    }

    // === Finalize ===

    @Transactional
    fun finalizeTransaction(
        transactionId: UUID,
        payments: List<PaymentInput>,
    ): TransactionEntity {
        tenantFilterService.enableFilter()
        val tx =
            transactionRepository.findById(transactionId)
                ?: throw IllegalArgumentException("Transaction not found: $transactionId")
        require(tx.status == "DRAFT") { "Transaction must be in DRAFT status to finalize, current: ${tx.status}" }

        val items = itemRepository.findByTransactionId(transactionId)
        require(items.isNotEmpty()) { "Transaction has no items" }

        recalculateTransactionTotals(tx)

        val paymentTotal = payments.sumOf { it.amount }
        require(paymentTotal >= tx.total) { "Payment total ($paymentTotal) is less than transaction total (${tx.total})" }

        for (input in payments) {
            val payment =
                PaymentEntity().apply {
                    this.organizationId = tx.organizationId
                    this.transactionId = transactionId
                    this.method = input.method
                    this.amount = input.amount
                    if (input.method == "CASH") {
                        this.received = input.received ?: input.amount
                        this.change = (input.received ?: input.amount) - input.amount
                    }
                    this.reference = input.reference
                }
            paymentRepository.persist(payment)
        }

        taxSummaryRepository.deleteByTransactionId(transactionId)
        val taxableItems =
            items.map {
                TaxableItem(
                    taxRateName = it.taxRateName,
                    taxRate = it.taxRate,
                    isReducedTax = it.isReducedTax,
                    subtotal = it.subtotal,
                )
            }
        val summaries = taxCalculationService.aggregateTaxSummaries(taxableItems)
        for (summary in summaries) {
            val entity =
                TaxSummaryEntity().apply {
                    this.organizationId = tx.organizationId
                    this.transactionId = transactionId
                    this.taxRateName = summary.taxRateName
                    this.taxRate = summary.taxRate
                    this.isReduced = summary.isReduced
                    this.taxableAmount = summary.taxableAmount
                    this.taxAmount = summary.taxAmount
                }
            taxSummaryRepository.persist(entity)
        }

        tx.status = "COMPLETED"
        tx.completedAt = Instant.now()
        transactionRepository.persist(tx)

        publishSaleCompletedEvent(tx, items)

        return tx
    }

    // === Void ===

    @Transactional
    fun voidTransaction(
        transactionId: UUID,
        reason: String,
    ): TransactionEntity {
        tenantFilterService.enableFilter()
        val tx =
            transactionRepository.findById(transactionId)
                ?: throw IllegalArgumentException("Transaction not found: $transactionId")
        require(tx.status == "COMPLETED") { "Only COMPLETED transactions can be voided, current: ${tx.status}" }
        require(reason.isNotBlank()) { "Void reason is required" }

        tx.status = "VOIDED"
        transactionRepository.persist(tx)

        val items = itemRepository.findByTransactionId(transactionId)
        publishSaleVoidedEvent(tx, items)

        return tx
    }

    // === Query ===

    fun getTransaction(transactionId: UUID): TransactionEntity {
        tenantFilterService.enableFilter()
        return transactionRepository.findById(transactionId)
            ?: throw IllegalArgumentException("Transaction not found: $transactionId")
    }

    fun getTransactionItems(transactionId: UUID): List<TransactionItemEntity> = itemRepository.findByTransactionId(transactionId)

    fun getTransactionPayments(transactionId: UUID): List<PaymentEntity> = paymentRepository.findByTransactionId(transactionId)

    fun getTransactionDiscounts(transactionId: UUID): List<TransactionDiscountEntity> =
        discountRepository.findByTransactionId(transactionId)

    fun getTransactionTaxSummaries(transactionId: UUID): List<TaxSummaryEntity> = taxSummaryRepository.findByTransactionId(transactionId)

    fun listTransactions(
        storeId: UUID?,
        terminalId: UUID?,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<TransactionEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val transactions = transactionRepository.listByFilters(storeId, terminalId, status, startDate, endDate, panachePage)
        val totalCount = transactionRepository.countByFilters(storeId, terminalId, status, startDate, endDate)
        return Pair(transactions, totalCount)
    }

    // === Batch Query (N+1 防止) ===

    /**
     * 複数取引の関連エンティティを一括取得する（N+1 クエリ防止）。
     * ListTransactions で取引一覧を返す際に、個別の取引ごとにクエリする代わりに
     * IN 句で一括取得して Map にグループ化する。
     */
    data class TransactionRelations(
        val items: Map<UUID, List<TransactionItemEntity>>,
        val payments: Map<UUID, List<PaymentEntity>>,
        val discounts: Map<UUID, List<TransactionDiscountEntity>>,
        val taxSummaries: Map<UUID, List<TaxSummaryEntity>>,
    )

    fun batchLoadRelations(transactionIds: List<UUID>): TransactionRelations {
        if (transactionIds.isEmpty()) {
            return TransactionRelations(emptyMap(), emptyMap(), emptyMap(), emptyMap())
        }
        val items = itemRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val payments = paymentRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val discounts = discountRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        val taxSummaries = taxSummaryRepository.findByTransactionIds(transactionIds).groupBy { it.transactionId }
        return TransactionRelations(items, payments, discounts, taxSummaries)
    }

    // === Internal Helpers ===

    private fun getWritableTransaction(transactionId: UUID): TransactionEntity {
        tenantFilterService.enableFilter()
        val tx =
            transactionRepository.findById(transactionId)
                ?: throw IllegalArgumentException("Transaction not found: $transactionId")
        require(tx.status == "DRAFT") { "Transaction is not in DRAFT status: ${tx.status}" }
        return tx
    }

    private fun recalculateTransactionTotals(tx: TransactionEntity) {
        val items = itemRepository.findByTransactionId(tx.id)
        val discounts = discountRepository.findByTransactionId(tx.id)

        tx.subtotal = items.sumOf { it.subtotal }
        tx.taxTotal = items.sumOf { it.taxAmount }
        tx.discountTotal = discounts.sumOf { it.amount }
        tx.total = tx.subtotal + tx.taxTotal - tx.discountTotal
        if (tx.total < 0) tx.total = 0
        transactionRepository.persist(tx)
    }

    private fun generateTransactionNumber(): String {
        val date = LocalDate.now(ZoneId.of("Asia/Tokyo"))
        val seq = (System.nanoTime() % 1_000_000).toString().padStart(6, '0')
        return "T-$date-$seq"
    }

    private fun publishSaleCompletedEvent(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
    ) {
        val event =
            SaleCompletedEventDto(
                transactionId = tx.id.toString(),
                storeId = tx.storeId.toString(),
                terminalId = tx.terminalId.toString(),
                items =
                    items.map { item ->
                        SaleItemDto(
                            productId = item.productId.toString(),
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            subtotal = item.subtotal,
                        )
                    },
                totalAmount = tx.total,
                transactedAt = tx.completedAt.toString(),
            )
        eventPublisher.publish("sale.completed", tx.organizationId, event)
    }

    private fun publishSaleVoidedEvent(
        tx: TransactionEntity,
        items: List<TransactionItemEntity>,
    ) {
        val event =
            SaleVoidedEventDto(
                originalTransactionId = tx.id.toString(),
                voidTransactionId = tx.id.toString(),
                storeId = tx.storeId.toString(),
                items =
                    items.map { item ->
                        SaleItemDto(
                            productId = item.productId.toString(),
                            quantity = item.quantity,
                            unitPrice = item.unitPrice,
                            subtotal = item.subtotal,
                        )
                    },
            )
        eventPublisher.publish("sale.voided", tx.organizationId, event)
    }
}

data class PaymentInput(
    val method: String,
    val amount: Long,
    val received: Long?,
    val reference: String?,
)
