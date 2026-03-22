package com.openpos.inventory.event

import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.repository.StockRepository
import com.openpos.inventory.service.PurchaseOrderService
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.UUID
import org.jboss.logging.Logger

/**
 * 在庫自動発注ハンドラー。
 * StockLowEvent を受信し、在庫が発注点以下の場合に自動で発注書を生成する。
 */
@ApplicationScoped
class AutoReorderHandler {
    @Inject
    lateinit var stockRepository: StockRepository

    @Inject
    lateinit var purchaseOrderService: PurchaseOrderService

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    companion object {
        private val logger: Logger = Logger.getLogger(AutoReorderHandler::class::class.java)
    }

    /**
     * 在庫低下イベントから自動発注を検討する。
     * reorderPoint > 0 かつ reorderQuantity > 0 の場合のみ自動発注を行う。
     */
    fun handleStockLow(
        organizationId: UUID,
        storeId: UUID,
        productId: UUID,
        currentQuantity: Int,
    ) {
        tenantFilterService.enableFilter()
        val stock = stockRepository.findByStoreAndProduct(storeId, productId) ?: return

        if (stock.reorderPoint <= 0 || stock.reorderQuantity <= 0) {
            logger.debug("Auto-reorder skipped: reorderPoint or reorderQuantity not configured for product=$productId")
            return
        }

        if (currentQuantity <= stock.reorderPoint) {
            logger.info(
                "Auto-reorder triggered: product=$productId, store=$storeId, " +
                    "current=$currentQuantity, reorderPoint=${stock.reorderPoint}, " +
                    "reorderQuantity=${stock.reorderQuantity}",
            )
            // 自動発注書を生成（プレースホルダー）
            // 本番では PurchaseOrderService を呼び出す
        }
    }
}
