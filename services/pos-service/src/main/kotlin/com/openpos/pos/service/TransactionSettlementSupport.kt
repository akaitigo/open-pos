package com.openpos.pos.service

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.event.EventPublisher
import com.openpos.pos.event.SaleCompletedEventDto
import com.openpos.pos.event.SaleItemDto
import com.openpos.pos.event.SalePaymentDto
import com.openpos.pos.event.SaleVoidedEventDto
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.TaxSummaryRepository
import java.security.MessageDigest
import java.util.UUID

internal data class TransactionTotals(
    val subtotal: Long,
    val taxTotal: Long,
    val discountTotal: Long,
    val total: Long,
)

internal fun updateTransactionTotals(
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
    discounts: List<TransactionDiscountEntity> = emptyList(),
) {
    val subtotal = items.sumOf { it.subtotal }
    val taxTotal = items.sumOf { it.taxAmount }
    val discountTotal = discounts.sumOf { it.amount }
    val total = (subtotal + taxTotal - discountTotal).coerceAtLeast(0)

    tx.subtotal = subtotal
    tx.taxTotal = taxTotal
    tx.discountTotal = discountTotal
    tx.total = total
}

internal fun persistPayments(
    organizationId: UUID,
    transactionId: UUID,
    payments: List<PaymentInput>,
    paymentRepository: PaymentRepository,
) {
    for (input in payments) {
        val payment =
            PaymentEntity().apply {
                this.organizationId = organizationId
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
}

internal fun rebuildTaxSummaries(
    organizationId: UUID,
    transactionId: UUID,
    items: List<TransactionItemEntity>,
    taxCalculationService: TaxCalculationService,
    taxSummaryRepository: TaxSummaryRepository,
) {
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
                this.organizationId = organizationId
                this.transactionId = transactionId
                this.taxRateName = summary.taxRateName
                this.taxRate = summary.taxRate
                this.isReduced = summary.isReduced
                this.taxableAmount = summary.taxableAmount
                this.taxAmount = summary.taxAmount
            }
        taxSummaryRepository.persist(entity)
    }
}

internal fun computeTransactionContentHash(
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
): String {
    val content =
        buildString {
            append(tx.id)
            append(tx.transactionNumber)
            append(tx.subtotal)
            append(tx.taxTotal)
            append(tx.discountTotal)
            append(tx.total)
            items.sortedBy { it.id }.forEach { item ->
                append(item.productId?.toString().orEmpty())
                append(item.productName)
                append(item.quantity)
                append(item.unitPrice)
                append(item.subtotal)
            }
        }

    val digest = MessageDigest.getInstance("SHA-256")
    return digest.digest(content.toByteArray()).joinToString("") { "%02x".format(it) }
}

internal fun publishSaleCompletedEvent(
    eventPublisher: EventPublisher,
    paymentRepository: PaymentRepository,
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
) {
    val payments = paymentRepository.findByTransactionId(tx.id)
    val event =
        SaleCompletedEventDto(
            transactionId = tx.id.toString(),
            storeId = tx.storeId.toString(),
            terminalId = tx.terminalId.toString(),
            items =
                items.map { item ->
                    SaleItemDto(
                        productId = item.productId?.toString(),
                        productName = item.productName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        subtotal = item.subtotal,
                    )
                },
            totalAmount = tx.total,
            taxTotal = tx.taxTotal,
            discountTotal = tx.discountTotal,
            payments =
                payments.map { payment ->
                    SalePaymentDto(
                        method = payment.method,
                        amount = payment.amount,
                    )
                },
            transactedAt = tx.completedAt.toString(),
        )
    eventPublisher.publish("sale.completed", tx.organizationId, event)
}

internal fun publishSaleVoidedEvent(
    eventPublisher: EventPublisher,
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
                        productId = item.productId?.toString(),
                        productName = item.productName,
                        quantity = item.quantity,
                        unitPrice = item.unitPrice,
                        subtotal = item.subtotal,
                    )
                },
            originalTransactedAt = (tx.completedAt ?: tx.createdAt).toString(),
        )
    eventPublisher.publish("sale.voided", tx.organizationId, event)
}
