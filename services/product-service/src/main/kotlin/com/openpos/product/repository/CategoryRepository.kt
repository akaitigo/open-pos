package com.openpos.product.repository

import com.openpos.product.entity.CategoryEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * カテゴリリポジトリ。
 * Hibernate Filter による organization_id フィルタリングは Service 層で有効化される前提。
 */
@ApplicationScoped
class CategoryRepository : PanacheRepositoryBase<CategoryEntity, UUID> {
    /**
     * 親カテゴリIDでカテゴリ一覧を取得する。
     * parentId が null の場合はルートカテゴリを返す。
     */
    fun findByParentId(parentId: UUID?): List<CategoryEntity> =
        if (parentId == null) {
            list("parentId IS NULL ORDER BY displayOrder ASC")
        } else {
            list("parentId = ?1 ORDER BY displayOrder ASC", parentId)
        }
}
