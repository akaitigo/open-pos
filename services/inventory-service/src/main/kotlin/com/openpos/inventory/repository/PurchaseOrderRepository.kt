package com.openpos.inventory.repository

import com.openpos.inventory.entity.PurchaseOrderEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 発注リポジトリ。
 * 店舗ごとの発注一覧、ステータスフィルタをサポートする。
 */
@ApplicationScoped
class PurchaseOrderRepository : PanacheRepositoryBase<PurchaseOrderEntity, UUID> {
    fun listByStoreId(
        storeId: UUID,
        status: String?,
        page: Page,
    ): List<PurchaseOrderEntity> {
        val conditions = mutableListOf("storeId = :storeId")
        val params = mutableMapOf<String, Any>("storeId" to storeId)
        if (!status.isNullOrBlank()) {
            conditions.add("status = :status")
            params["status"] = status
        }
        return find(conditions.joinToString(" AND "), Sort.descending("createdAt"), params)
            .page(page)
            .list()
    }

    fun countByStoreId(
        storeId: UUID,
        status: String?,
    ): Long {
        val conditions = mutableListOf("storeId = :storeId")
        val params = mutableMapOf<String, Any>("storeId" to storeId)
        if (!status.isNullOrBlank()) {
            conditions.add("status = :status")
            params["status"] = status
        }
        return count(conditions.joinToString(" AND "), params)
    }
}
