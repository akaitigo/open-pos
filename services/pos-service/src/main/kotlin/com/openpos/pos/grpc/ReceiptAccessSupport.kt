package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import openpos.pos.v1.Receipt
import java.util.UUID

internal data class ReceiptTransactionView(
    val transaction: TransactionEntity,
    val items: List<TransactionItemEntity>,
    val payments: List<PaymentEntity>,
    val taxSummaries: List<TaxSummaryEntity>,
)

internal data class InvoiceReceiptView(
    val receipt: ReceiptTransactionView,
    val invoiceNumber: String,
)

internal fun ReceiptTransactionView.toReceiptProto(): Receipt =
    buildReceiptProto(
        transaction,
        items,
        payments,
        taxSummaries,
    )

internal fun InvoiceReceiptView.toReceiptData(): String =
    buildInvoiceReceiptData(
        receipt.transaction,
        receipt.items,
        receipt.payments,
        receipt.taxSummaries,
        invoiceNumber,
    )

internal fun loadReceiptTransactionView(
    transactionId: UUID,
    loadTransaction: (UUID) -> TransactionEntity,
    loadItems: (UUID) -> List<TransactionItemEntity>,
    loadPayments: (UUID) -> List<PaymentEntity>,
    loadTaxSummaries: (UUID) -> List<TaxSummaryEntity>,
): ReceiptTransactionView {
    val transaction = loadTransaction(transactionId)
    require(transaction.status == "COMPLETED" || transaction.status == "VOIDED") {
        "Receipt is only available for COMPLETED or VOIDED transactions"
    }

    return hydrateReceiptTransactionView(transaction, loadItems, loadPayments, loadTaxSummaries)
}

internal fun loadInvoiceReceiptView(
    transactionId: UUID,
    loadTransaction: (UUID) -> TransactionEntity,
    loadItems: (UUID) -> List<TransactionItemEntity>,
    loadPayments: (UUID) -> List<PaymentEntity>,
    loadTaxSummaries: (UUID) -> List<TaxSummaryEntity>,
    getInvoiceNumber: (UUID) -> String?,
): InvoiceReceiptView {
    val transaction = loadTransaction(transactionId)
    require(transaction.status == "COMPLETED") {
        "Invoice receipt is only available for COMPLETED transactions"
    }

    val receipt = hydrateReceiptTransactionView(transaction, loadItems, loadPayments, loadTaxSummaries)
    val invoiceNumber =
        getInvoiceNumber(transaction.organizationId)
            ?: throw IllegalArgumentException(
                "Invoice registration number is not configured for organization: ${transaction.organizationId}",
            )

    return InvoiceReceiptView(
        receipt = receipt,
        invoiceNumber = invoiceNumber,
    )
}

private fun hydrateReceiptTransactionView(
    transaction: TransactionEntity,
    loadItems: (UUID) -> List<TransactionItemEntity>,
    loadPayments: (UUID) -> List<PaymentEntity>,
    loadTaxSummaries: (UUID) -> List<TaxSummaryEntity>,
): ReceiptTransactionView =
    ReceiptTransactionView(
        transaction = transaction,
        items = loadItems(transaction.id),
        payments = loadPayments(transaction.id),
        taxSummaries = loadTaxSummaries(transaction.id),
    )
