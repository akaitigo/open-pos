package com.openpos.pos.event

/**
 * 売上完了イベントペイロード。
 * FinalizeTransaction 時に発行される。
 */
data class SaleCompletedEventDto(
    val transactionId: String,
    val storeId: String,
    val terminalId: String,
    val items: List<SaleItemDto>,
    val totalAmount: Long,
    val transactedAt: String,
)

data class SaleItemDto(
    val productId: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotal: Long,
)
