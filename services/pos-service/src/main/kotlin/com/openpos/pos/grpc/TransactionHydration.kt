package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionDiscountEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import com.openpos.pos.service.TransactionService
import openpos.pos.v1.Transaction

internal fun TransactionEntity.toFullProto(transactionService: TransactionService): Transaction {
    val items = transactionService.getTransactionItems(id)
    val payments = transactionService.getTransactionPayments(id)
    val discounts = transactionService.getTransactionDiscounts(id)
    val taxSummaries = transactionService.getTransactionTaxSummaries(id)
    return toProto(items, payments, discounts, taxSummaries)
}

internal fun TransactionEntity.toBatchProto(
    items: List<TransactionItemEntity>,
    payments: List<PaymentEntity>,
    discounts: List<TransactionDiscountEntity>,
    taxSummaries: List<TaxSummaryEntity>,
): Transaction = toProto(items, payments, discounts, taxSummaries)
