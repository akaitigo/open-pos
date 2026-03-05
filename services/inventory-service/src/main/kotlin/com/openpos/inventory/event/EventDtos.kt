package com.openpos.inventory.event

// Re-use same DTO structure as pos-service (shared via JSON contract)
data class EventEnvelopeDto(
    val eventId: String,
    val eventType: String,
    val timestamp: String,
    val organizationId: String,
    val payload: String,
    val source: String,
)

data class SaleCompletedPayload(
    val transactionId: String,
    val storeId: String,
    val terminalId: String,
    val items: List<SaleItemPayload>,
    val totalAmount: Long,
    val transactedAt: String,
)

data class SaleVoidedPayload(
    val originalTransactionId: String,
    val voidTransactionId: String,
    val storeId: String,
    val items: List<SaleItemPayload>,
)

data class SaleItemPayload(
    val productId: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotal: Long,
)
