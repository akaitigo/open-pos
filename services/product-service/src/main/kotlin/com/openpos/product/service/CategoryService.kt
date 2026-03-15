package com.openpos.product.service

import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CategoryEntity
import com.openpos.product.repository.CategoryRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * カテゴリのビジネスロジック層。
 * CRUD と階層構造の取得を提供する。
 * cache-aside パターンで Redis キャッシュを利用する。
 */
@ApplicationScoped
class CategoryService {
    @Inject
    lateinit var categoryRepository: CategoryRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var cacheService: ProductCacheService

    /**
     * カテゴリを作成する。作成後にカテゴリリストキャッシュを無効化する。
     */
    @Transactional
    fun create(
        name: String,
        parentId: UUID?,
        color: String?,
        icon: String?,
        displayOrder: Int,
    ): CategoryEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            CategoryEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.parentId = parentId
                this.color = color
                this.icon = icon
                this.displayOrder = displayOrder
            }
        categoryRepository.persist(entity)
        cacheService.invalidateAllCategoryLists()
        return entity
    }

    /**
     * カテゴリ一覧を取得する。parentId が null の場合はルートカテゴリを返す。
     */
    fun listByParentId(parentId: UUID?): List<CategoryEntity> {
        tenantFilterService.enableFilter()
        return categoryRepository.findByParentId(parentId)
    }

    /**
     * IDでカテゴリを取得する。
     */
    fun findById(id: UUID): CategoryEntity? {
        tenantFilterService.enableFilter()
        return categoryRepository.findById(id)
    }

    /**
     * カテゴリを更新する。更新後にキャッシュを無効化する。
     */
    @Transactional
    fun update(
        id: UUID,
        name: String?,
        parentId: UUID?,
        color: String?,
        icon: String?,
        displayOrder: Int?,
    ): CategoryEntity? {
        tenantFilterService.enableFilter()
        val entity = categoryRepository.findById(id) ?: return null

        name?.let { entity.name = it }
        // parentId は nullable なので明示的にセット（空文字列の場合も想定）
        if (parentId != null) {
            entity.parentId = parentId
        }
        color?.let { entity.color = it }
        icon?.let { entity.icon = it }
        displayOrder?.let { entity.displayOrder = it }

        categoryRepository.persist(entity)
        cacheService.invalidateCategory(id.toString())
        return entity
    }

    /**
     * カテゴリを削除する。削除後にキャッシュを無効化する。
     */
    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = categoryRepository.findById(id) ?: return false
        categoryRepository.delete(entity)
        cacheService.invalidateCategory(id.toString())
        return true
    }
}
