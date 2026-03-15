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
 * 店舗×商品×日の売上集計データを検索・更新する。
 */
@ApplicationScoped
class ProductSalesRepository : PanacheRepositoryBase<ProductSalesEntity, UUID> {
    /**
     * 店舗×商品×日で売上レコードを検索する。
     */
    fun findByStoreProductAndDate(
        storeId: UUID,
        productId: UUID,
        saleDate: LocalDate,
    ): ProductSalesEntity? = find("storeId = ?1 AND productId = ?2 AND saleDate = ?3", storeId, productId, saleDate).firstResult()

    /**
     * 店舗の日付範囲で商品別売上を取得する（集約）。
     * productId が指定された場合はその商品のみ。
     */
    fun listByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
        sortBy: String?,
        page: Page,
    ): List<ProductSalesEntity> {
        val sort =
            when (sortBy) {
                "quantity" -> Sort.descending("quantitySold")
                "amount" -> Sort.descending("totalAmount")
                else -> Sort.descending("totalAmount")
            }

        return if (productId != null) {
            find(
                "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3 AND productId = ?4",
                sort,
                storeId,
                startDate,
                endDate,
                productId,
            ).page(page).list()
        } else {
            find(
                "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3",
                sort,
                storeId,
                startDate,
                endDate,
            ).page(page).list()
        }
    }

    /**
     * 店舗の日付範囲の商品別売上件数を取得する。
     */
    fun countByStoreAndDateRange(
        storeId: UUID,
        startDate: LocalDate,
        endDate: LocalDate,
        productId: UUID?,
    ): Long =
        if (productId != null) {
            count(
                "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3 AND productId = ?4",
                storeId,
                startDate,
                endDate,
                productId,
            )
        } else {
            count(
                "storeId = ?1 AND saleDate >= ?2 AND saleDate <= ?3",
                storeId,
                startDate,
                endDate,
            )
        }
}
