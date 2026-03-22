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
    val taxTotal: Long = 0,
    val discountTotal: Long = 0,
    val payments: List<SalePaymentDto> = emptyList(),
    val transactedAt: String,
)

data class SalePaymentDto(
    val method: String,
    val amount: Long,
)

data class SaleItemDto(
    val productId: String,
    val productName: String,
    val quantity: Int,
    val unitPrice: Long,
    val subtotal: Long,
)
