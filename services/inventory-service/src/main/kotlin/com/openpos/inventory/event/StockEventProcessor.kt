package com.openpos.inventory.event

import com.openpos.inventory.config.OrganizationIdHolder
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

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun processSaleCompleted(
        organizationId: UUID,
        payload: SaleCompletedPayload,
    ) {
        organizationIdHolder.organizationId = organizationId
        try {
            val storeId = UUID.fromString(payload.storeId)
            for (item in payload.items) {
                val pid = item.productId
                if (pid.isNullOrBlank()) continue // カスタムアイテムは在庫操作不要
                stockService.adjustStock(
                    storeId = storeId,
                    productId = UUID.fromString(pid),
                    quantityChange = -item.quantity,
                    movementType = "SALE",
                    referenceId = payload.transactionId,
                    note = null,
                )
            }
        } finally {
            organizationIdHolder.clear()
        }
    }

    fun processSaleVoided(
        organizationId: UUID,
        payload: SaleVoidedPayload,
    ) {
        organizationIdHolder.organizationId = organizationId
        try {
            val storeId = UUID.fromString(payload.storeId)
            for (item in payload.items) {
                val pid = item.productId
                if (pid.isNullOrBlank()) continue // カスタムアイテムは在庫操作不要
                stockService.adjustStock(
                    storeId = storeId,
                    productId = UUID.fromString(pid),
                    quantityChange = item.quantity,
                    movementType = "RETURN",
                    referenceId = payload.voidTransactionId,
                    note = "Void of transaction ${payload.originalTransactionId}",
                )
            }
        } finally {
            organizationIdHolder.clear()
        }
    }
}
