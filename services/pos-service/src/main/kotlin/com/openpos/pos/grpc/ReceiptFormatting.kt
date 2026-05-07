package com.openpos.pos.grpc

import com.openpos.pos.entity.PaymentEntity
import com.openpos.pos.entity.TaxSummaryEntity
import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.entity.TransactionItemEntity
import openpos.pos.v1.Receipt
import java.time.Instant
import java.util.UUID

internal fun buildReceiptProto(
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
    payments: List<PaymentEntity>,
    taxSummaries: List<TaxSummaryEntity>,
    now: Instant = Instant.now(),
    receiptId: String = UUID.randomUUID().toString(),
): Receipt =
    Receipt
        .newBuilder()
        .setId(receiptId)
        .setTransactionId(tx.id.toString())
        .setReceiptData(buildReceiptData(tx, items, payments, taxSummaries))
        .setCreatedAt(now.toString())
        .build()

internal fun buildInvoiceReceiptData(
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
    payments: List<PaymentEntity>,
    taxSummaries: List<TaxSummaryEntity>,
    invoiceNumber: String,
): String {
    val sb = StringBuilder()
    sb.appendLine("================================")
    sb.appendLine("        領 収 書")
    sb.appendLine("  （適格簡易請求書）")
    sb.appendLine("================================")
    sb.appendLine("取引番号: ${tx.transactionNumber}")
    sb.appendLine("日時: ${tx.completedAt}")
    sb.appendLine("登録番号: $invoiceNumber")
    sb.appendLine("--------------------------------")
    for (item in items) {
        val reducedMark = if (item.isReducedTax) " ※" else ""
        sb.appendLine("${item.productName}$reducedMark")
        sb.appendLine("  ${item.unitPrice / 100}円 x ${item.quantity}  ${item.total / 100}円")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("小計:     ${tx.subtotal / 100}円")
    sb.appendLine("消費税:   ${tx.taxTotal / 100}円")
    if (tx.discountTotal > 0) {
        sb.appendLine("割引:    -${tx.discountTotal / 100}円")
    }
    sb.appendLine("合計:     ${tx.total / 100}円")
    sb.appendLine("--------------------------------")
    for (payment in payments) {
        sb.appendLine("${payment.method}: ${payment.amount / 100}円")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("【税率別内訳（税込）】")
    for (summary in taxSummaries) {
        val reducedMark = if (summary.isReduced) "※" else ""
        sb.appendLine("$reducedMark${summary.taxRateName}")
        sb.appendLine("  対象: ${summary.taxableAmount / 100}円  税: ${summary.taxAmount / 100}円")
    }
    if (taxSummaries.any { it.isReduced }) {
        sb.appendLine("※は軽減税率(8%)対象商品")
    }
    sb.appendLine("================================")
    sb.appendLine("上記正に領収いたしました")
    sb.appendLine("================================")
    return sb.toString()
}

internal fun buildReceiptData(
    tx: TransactionEntity,
    items: List<TransactionItemEntity>,
    payments: List<PaymentEntity>,
    taxSummaries: List<TaxSummaryEntity>,
): String {
    val sb = StringBuilder()
    sb.appendLine("================================")
    sb.appendLine("        領 収 書")
    sb.appendLine("================================")
    sb.appendLine("取引番号: ${tx.transactionNumber}")
    sb.appendLine("日時: ${tx.completedAt}")
    sb.appendLine("--------------------------------")
    for (item in items) {
        val reducedMark = if (item.isReducedTax) " ※" else ""
        sb.appendLine("${item.productName}$reducedMark")
        sb.appendLine("  ${item.unitPrice / 100}円 x ${item.quantity}  ${item.total / 100}円")
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("小計:     ${tx.subtotal / 100}円")
    sb.appendLine("消費税:   ${tx.taxTotal / 100}円")
    if (tx.discountTotal > 0) {
        sb.appendLine("割引:    -${tx.discountTotal / 100}円")
    }
    sb.appendLine("合計:     ${tx.total / 100}円")
    sb.appendLine("--------------------------------")
    for (payment in payments) {
        sb.appendLine("${payment.method}: ${payment.amount / 100}円")
        val changeVal = payment.change ?: 0
        if (payment.method == "CASH" && changeVal > 0) {
            sb.appendLine("お釣り: ${changeVal / 100}円")
        }
    }
    sb.appendLine("--------------------------------")
    sb.appendLine("【税率別内訳】")
    for (summary in taxSummaries) {
        val reducedMark = if (summary.isReduced) "※" else ""
        sb.appendLine("$reducedMark${summary.taxRateName}: 対象${summary.taxableAmount / 100}円 税${summary.taxAmount / 100}円")
    }
    if (taxSummaries.any { it.isReduced }) {
        sb.appendLine("※は軽減税率対象商品")
    }
    sb.appendLine("================================")
    return sb.toString()
}
