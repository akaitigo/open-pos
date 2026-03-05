package com.openpos.inventory.repository

import com.openpos.inventory.entity.StockEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 在庫リポジトリ。
 * 店舗×商品の在庫検索、在庫低下フィルタをサポートする。
 */
@ApplicationScoped
class StockRepository : PanacheRepositoryBase<StockEntity, UUID> {
    /**
     * 店舗×商品で在庫を検索する。
     */
    fun findByStoreAndProduct(
        storeId: UUID,
        productId: UUID,
    ): StockEntity? = find("storeId = ?1 AND productId = ?2", storeId, productId).firstResult()

    /**
     * 店舗の在庫一覧を取得する（ページネーション対応）。
     */
    fun listByStoreId(
        storeId: UUID,
        page: Page,
    ): List<StockEntity> =
        find("storeId = ?1", Sort.ascending("productId"), storeId)
            .page(page)
            .list()

    /**
     * 店舗の在庫低下商品を取得する（quantity <= lowStockThreshold）。
     */
    fun listLowStock(
        storeId: UUID,
        page: Page,
    ): List<StockEntity> =
        find("storeId = ?1 AND quantity <= lowStockThreshold", Sort.ascending("quantity"), storeId)
            .page(page)
            .list()

    /**
     * 店舗の在庫総件数を取得する。
     */
    fun countByStoreId(storeId: UUID): Long = count("storeId = ?1", storeId)

    /**
     * 店舗の在庫低下商品の件数を取得する。
     */
    fun countLowStock(storeId: UUID): Long = count("storeId = ?1 AND quantity <= lowStockThreshold", storeId)
}
