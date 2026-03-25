package com.openpos.product.service

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CategoryEntity
import com.openpos.product.repository.CategoryRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.util.UUID

/**
 * カテゴリのビジネスロジック層。
 * CRUD と階層構造の取得を提供する。
 * cache-aside パターンで Redis キャッシュを利用する。
 * キー形式: openpos:product-service:{orgId}:category:{id}
 * リストキー: openpos:product-service:{orgId}:category:list:{parentId}
 * TTL: 3600 秒（1 時間）
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

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(CategoryService::class.java)

    companion object {
        const val CATEGORY_TTL_SECONDS = 3600L
    }

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
        cacheService.invalidateAllCategoryLists(orgId.toString())
        return entity
    }

    /**
     * カテゴリ一覧を取得する。parentId が null の場合はルートカテゴリを返す。
     * cache-aside: Redis にキャッシュがあればそこから返し、なければ DB から取得してキャッシュする。
     */
    fun listByParentId(parentId: UUID?): List<CategoryEntity> {
        tenantFilterService.enableFilter()
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        val cacheKey = cacheService.categoryListKey(orgId.toString(), parentId?.toString())

        // cache-aside: キャッシュ確認
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return try {
                objectMapper.readValue(cached, object : TypeReference<List<CategoryCacheDto>>() {}).map { dto ->
                    CategoryEntity().apply {
                        this.id = dto.id
                        this.organizationId = dto.organizationId
                        this.name = dto.name
                        this.parentId = dto.parentId
                        this.color = dto.color
                        this.icon = dto.icon
                        this.displayOrder = dto.displayOrder
                    }
                }
            } catch (e: Exception) {
                log.warnf("Failed to deserialize category list cache: %s", e.message)
                categoryRepository.findByParentId(parentId)
            }
        }

        // cache miss: DB から取得
        val categories = categoryRepository.findByParentId(parentId)

        // キャッシュに書き込み
        try {
            val dtos = categories.map { CategoryCacheDto.from(it) }
            cacheService.set(cacheKey, objectMapper.writeValueAsString(dtos), CATEGORY_TTL_SECONDS)
        } catch (e: Exception) {
            log.warnf("Failed to cache category list: %s", e.message)
        }

        return categories
    }

    /**
     * IDでカテゴリを取得する。
     * cache-aside: Redis にキャッシュがあればそこから返す。
     */
    fun findById(id: UUID): CategoryEntity? {
        tenantFilterService.enableFilter()
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        val cacheKey = cacheService.categoryKey(orgId.toString(), id.toString())

        // cache-aside: キャッシュ確認
        val cached = cacheService.get(cacheKey)
        if (cached != null) {
            return try {
                val dto = objectMapper.readValue(cached, CategoryCacheDto::class.java)
                CategoryEntity().apply {
                    this.id = dto.id
                    this.organizationId = dto.organizationId
                    this.name = dto.name
                    this.parentId = dto.parentId
                    this.color = dto.color
                    this.icon = dto.icon
                    this.displayOrder = dto.displayOrder
                }
            } catch (e: Exception) {
                log.warnf("Failed to deserialize category cache: %s", e.message)
                categoryRepository.findById(id)
            }
        }

        // cache miss: DB から取得
        val entity = categoryRepository.findById(id) ?: return null

        // キャッシュに書き込み
        try {
            cacheService.set(cacheKey, objectMapper.writeValueAsString(CategoryCacheDto.from(entity)), CATEGORY_TTL_SECONDS)
        } catch (e: Exception) {
            log.warnf("Failed to cache category: %s", e.message)
        }

        return entity
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
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
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
        cacheService.invalidateCategory(orgId.toString(), id.toString())
        return entity
    }

    /**
     * カテゴリを削除する。削除後にキャッシュを無効化する。
     */
    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }
        val entity = categoryRepository.findById(id) ?: return false
        categoryRepository.delete(entity)
        cacheService.invalidateCategory(orgId.toString(), id.toString())
        return true
    }
}

/**
 * Redis シリアライズ用の DTO。
 * JPA エンティティを直接シリアライズしない。
 */
data class CategoryCacheDto(
    val id: UUID,
    val organizationId: UUID,
    val name: String,
    val parentId: UUID?,
    val color: String?,
    val icon: String?,
    val displayOrder: Int,
) {
    companion object {
        fun from(entity: CategoryEntity): CategoryCacheDto =
            CategoryCacheDto(
                id = entity.id,
                organizationId = entity.organizationId,
                name = entity.name,
                parentId = entity.parentId,
                color = entity.color,
                icon = entity.icon,
                displayOrder = entity.displayOrder,
            )
    }
}
