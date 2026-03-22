package com.openpos.analytics.event

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

// Re-use same DTO structure as pos-service (shared via JSON contract)
@JsonIgnoreProperties(ignoreUnknown = true)
data class EventEnvelopeDto(
    val eventId: String,
    val eventType: String,
    val timestamp: String,
    val organizationId: String,
    val payload: String,
    val source: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SaleCompletedPayload(
    val transactionId: String,
    val storeId: String,
    val terminalId: String,
    val items: List<SaleItemPayload>,
    val totalAmount: Long,
    val transactedAt: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SaleVoidedPayload(
    val originalTransactionId: String,
    val voidTransactionId: String,
    val storeId: String,
    val items: List<SaleItemPayload>,
    val originalTransactedAt: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SaleItemPayload(
    val productId: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotal: Long,
    val productName: String? = null,
)
