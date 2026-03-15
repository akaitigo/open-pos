package com.openpos.analytics.repository

import com.openpos.analytics.entity.ProductSalesEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

/**
 * 商品別売上リポジトリ。
 */
@ApplicationScoped
class ProductSalesRepository : PanacheRepositoryBase<ProductSalesEntity, UUID> {
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
}
