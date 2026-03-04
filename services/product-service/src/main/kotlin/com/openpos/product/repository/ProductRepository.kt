package com.openpos.product.repository

import com.openpos.product.entity.ProductEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 商品リポジトリ。
 * バーコード検索、カテゴリ検索、フリーワード検索をサポートする。
 */
@ApplicationScoped
class ProductRepository : PanacheRepositoryBase<ProductEntity, UUID> {
    /**
     * バーコードで商品を検索する（組織内で一意）。
     */
    fun findByBarcode(barcode: String): ProductEntity? = find("barcode = ?1", barcode).firstResult()

    /**
     * カテゴリIDで商品一覧を取得する。
     */
    fun findByCategoryId(categoryId: UUID): List<ProductEntity> = list("categoryId = ?1 ORDER BY displayOrder ASC", categoryId)

    /**
     * フリーワード検索（商品名・バーコード・SKU に部分一致）。
     * ページネーション対応。
     *
     * @param query 検索文字列（null または空文字の場合は全件）
     * @param categoryId カテゴリID（null の場合はフィルタなし）
     * @param activeOnly true の場合は is_active=true のみ
     * @param page ページネーション情報
     * @return 商品リスト
     */
    fun search(
        query: String?,
        categoryId: UUID?,
        activeOnly: Boolean,
        page: Page,
    ): List<ProductEntity> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (!query.isNullOrBlank()) {
            conditions.add("(LOWER(name) LIKE :query OR LOWER(barcode) LIKE :query OR LOWER(sku) LIKE :query)")
            params["query"] = "%${query.lowercase()}%"
        }

        if (categoryId != null) {
            conditions.add("categoryId = :categoryId")
            params["categoryId"] = categoryId
        }

        if (activeOnly) {
            conditions.add("isActive = true")
        }

        val whereClause = if (conditions.isEmpty()) "" else conditions.joinToString(" AND ")

        return find(whereClause, Sort.ascending("displayOrder", "name"), params)
            .page(page)
            .list()
    }

    /**
     * 検索条件に合致する商品の総件数を取得する。
     */
    fun searchCount(
        query: String?,
        categoryId: UUID?,
        activeOnly: Boolean,
    ): Long {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()

        if (!query.isNullOrBlank()) {
            conditions.add("(LOWER(name) LIKE :query OR LOWER(barcode) LIKE :query OR LOWER(sku) LIKE :query)")
            params["query"] = "%${query.lowercase()}%"
        }

        if (categoryId != null) {
            conditions.add("categoryId = :categoryId")
            params["categoryId"] = categoryId
        }

        if (activeOnly) {
            conditions.add("isActive = true")
        }

        val whereClause = if (conditions.isEmpty()) "" else conditions.joinToString(" AND ")

        return count(whereClause, params)
    }
}
