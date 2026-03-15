package com.openpos.analytics.repository

import com.openpos.analytics.entity.ProductSalesEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

/**
 * 商品別売上リポジトリ。
 */
@ApplicationScoped
class ProductSalesRepository : PanacheRepositoryBase<ProductSalesEntity, UUID> {
    /**
     * 指定店舗・商品・日で売上レコードを検索する。
     */
    fun findByStoreProductAndDate(
        storeId: UUID,
        productId: UUID,
        saleDate: LocalDate,
    ): ProductSalesEntity? = find("storeId = ?1 AND productId = ?2 AND date = ?3", storeId, productId, saleDate).firstResult()

    /**
     * 指定店舗・期間の商品別売上集計を取得する。
     * product_id でグルーピングし、数量と金額を合算する。
     */
    fun findAggregatedByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<ProductSalesEntity> =
        list(
            "storeId = ?1 AND date >= ?2 AND date <= ?3",
            storeId,
            startDate,
            endDate,
        )

    /**
     * 指定店舗・期間の商品別売上を取得する（ページネーション・ソート対応）。
     */
    fun listByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
        sortBy: String?,
        page: Page,
    ): List<ProductSalesEntity> {
        val (query, params) = buildFilterQuery(storeId, startDate, endDate, productId)
        val sort =
            when (sortBy) {
                "quantity" -> Sort.descending("quantitySold")
                else -> Sort.descending("totalAmount")
            }
        return find(query, sort, params).page(page).list()
    }

    /**
     * 指定店舗・期間の商品別売上の件数を取得する。
     */
    fun countByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
    ): Long {
        val (query, params) = buildFilterQuery(storeId, startDate, endDate, productId)
        return count(query, params)
    }

    private fun buildFilterQuery(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
    ): Pair<String, Map<String, Any>> {
        val conditions = mutableListOf("storeId = :storeId", "date >= :startDate", "date <= :endDate")
        val params = mutableMapOf<String, Any>("storeId" to storeId, "startDate" to startDate, "endDate" to endDate)
        if (productId != null) {
            conditions.add("productId = :productId")
            params["productId"] = productId
        }
        return Pair(conditions.joinToString(" AND "), params)
    }
}
