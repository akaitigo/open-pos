package com.openpos.pos.event

data class SaleVoidedEventDto(
    val originalTransactionId: String,
    val voidTransactionId: String,
    val storeId: String,
    val items: List<SaleItemDto>,
)
