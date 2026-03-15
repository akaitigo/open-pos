package com.openpos.inventory.repository

import com.openpos.inventory.entity.PurchaseOrderItemEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 発注明細リポジトリ。
 */
@ApplicationScoped
class PurchaseOrderItemRepository : PanacheRepositoryBase<PurchaseOrderItemEntity, UUID> {
    fun findByPurchaseOrderId(purchaseOrderId: UUID): List<PurchaseOrderItemEntity> = list("purchaseOrderId = ?1", purchaseOrderId)
}
