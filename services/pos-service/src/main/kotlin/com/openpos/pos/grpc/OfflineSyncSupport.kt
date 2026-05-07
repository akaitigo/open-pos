package com.openpos.pos.grpc

import com.openpos.pos.entity.TransactionEntity
import com.openpos.pos.service.OfflineItemInput
import com.openpos.pos.service.PaymentInput
import openpos.pos.v1.OfflineTransaction
import openpos.pos.v1.SyncResult
import java.time.Instant
import java.util.UUID

internal data class OfflineTransactionSyncInput(
    val clientId: String,
    val storeId: UUID,
    val terminalId: UUID,
    val staffId: UUID,
    val items: List<OfflineItemInput>,
    val payments: List<PaymentInput>,
    val createdAt: Instant?,
)

internal fun syncOfflineTransactionResult(
    offlineTransaction: OfflineTransaction,
    syncTransaction: (OfflineTransactionSyncInput) -> TransactionEntity,
): SyncResult =
    try {
        val input = offlineTransaction.toSyncInput()
        val entity = syncTransaction(input)
        SyncResult
            .newBuilder()
            .setClientId(input.clientId)
            .setSuccess(true)
            .setTransactionId(entity.id.toString())
            .build()
    } catch (e: Exception) {
        SyncResult
            .newBuilder()
            .setClientId(offlineTransaction.clientId)
            .setSuccess(false)
            .setError(e.message ?: "Unknown error")
            .build()
    }

private fun OfflineTransaction.toSyncInput(): OfflineTransactionSyncInput =
    OfflineTransactionSyncInput(
        clientId = clientId,
        storeId = storeId.toUUID(),
        terminalId = terminalId.toUUID(),
        staffId = staffId.toUUID(),
        items =
            itemsList.map { item ->
                OfflineItemInput(
                    productId = item.productId.toUUID(),
                    productName = item.productName,
                    unitPrice = item.unitPrice,
                    quantity = item.quantity,
                    taxRateName = item.taxRateName,
                    taxRate = item.taxRate,
                    isReducedTax = item.isReducedTax,
                )
            },
        payments =
            paymentsList.map { payment ->
                PaymentInput(
                    method = payment.method.toDbValue(),
                    amount = payment.amount,
                    received = if (payment.received > 0) payment.received else null,
                    reference = payment.reference.ifBlank { null },
                )
            },
        createdAt =
            if (createdAt.isNotBlank()) {
                Instant.parse(createdAt)
            } else {
                null
            },
    )
