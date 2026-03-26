package com.openpos.inventory.repository

import com.openpos.inventory.entity.StockMovementEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

/**
 * 在庫移動履歴リポジトリ。
 * 店舗×商品の移動履歴検索、日付範囲フィルタをサポートする。
 */
@ApplicationScoped
class StockMovementRepository : PanacheRepositoryBase<StockMovementEntity, UUID> {
    /**
     * 店舗（+商品）の移動履歴を取得する（ページネーション対応）。
     * productId が null の場合は店舗全商品の履歴を返す。
     */
    fun listByStoreAndProduct(
        storeId: UUID,
        productId: UUID?,
        startDate: Instant?,
        endDate: Instant?,
        page: Page,
    ): List<StockMovementEntity> {
        val conditions = mutableListOf("storeId = :storeId")
        val params = mutableMapOf<String, Any>("storeId" to storeId)

        if (productId != null) {
            conditions.add("productId = :productId")
            params["productId"] = productId
        }

        if (startDate != null) {
            conditions.add("createdAt >= :startDate")
            params["startDate"] = startDate
        }

        if (endDate != null) {
            conditions.add("createdAt <= :endDate")
            params["endDate"] = endDate
        }

        val whereClause = conditions.joinToString(" AND ")

        return find(whereClause, Sort.descending("createdAt"), params)
            .page(page)
            .list()
    }

    /**
     * 店舗（+商品）の移動履歴件数を取得する。
     */
    fun countByStoreAndProduct(
        storeId: UUID,
        productId: UUID?,
        startDate: Instant?,
        endDate: Instant?,
    ): Long {
        val conditions = mutableListOf("storeId = :storeId")
        val params = mutableMapOf<String, Any>("storeId" to storeId)

        if (productId != null) {
            conditions.add("productId = :productId")
            params["productId"] = productId
        }

        if (startDate != null) {
            conditions.add("createdAt >= :startDate")
            params["startDate"] = startDate
        }

        if (endDate != null) {
            conditions.add("createdAt <= :endDate")
            params["endDate"] = endDate
        }

        val whereClause = conditions.joinToString(" AND ")

        return count(whereClause, params)
    }

    /**
     * referenceId と movementType で移動履歴を検索する（冪等性チェック用）。
     */
    fun findByReferenceIdAndMovementType(
        referenceId: String,
        movementType: String,
    ): StockMovementEntity? =
        find(
            "referenceId = :referenceId AND movementType = :movementType",
            mapOf(
                "referenceId" to referenceId,
                "movementType" to movementType,
            ),
        ).firstResult()
}
