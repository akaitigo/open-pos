package com.openpos.inventory.event

import com.openpos.inventory.service.StockService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.UUID

/**
 * イベントペイロードを在庫操作に変換する。
 */
@ApplicationScoped
class StockEventProcessor {
    @Inject
    lateinit var stockService: StockService

    fun processSaleCompleted(
        organizationId: UUID,
        payload: SaleCompletedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        for (item in payload.items) {
            stockService.adjustStock(
                storeId = storeId,
                productId = UUID.fromString(item.productId),
                quantityChange = -item.quantity,
                movementType = "SALE",
                referenceId = payload.transactionId,
                note = null,
            )
        }
    }

    fun processSaleVoided(
        organizationId: UUID,
        payload: SaleVoidedPayload,
    ) {
        val storeId = UUID.fromString(payload.storeId)
        for (item in payload.items) {
            stockService.adjustStock(
                storeId = storeId,
                productId = UUID.fromString(item.productId),
                quantityChange = item.quantity,
                movementType = "RETURN",
                referenceId = payload.voidTransactionId,
                note = "Void of transaction ${payload.originalTransactionId}",
            )
        }
    }
}
