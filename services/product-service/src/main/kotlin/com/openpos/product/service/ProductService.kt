package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductEntity
import com.openpos.product.repository.ProductRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * 商品のビジネスロジック層。
 * CRUD、バーコード検索、フリーワード検索、ページネーションを提供する。
 */
@ApplicationScoped
class ProductService {
    @Inject
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * 商品を作成する。
     */
    @Transactional
    fun create(
        name: String,
        description: String?,
        barcode: String?,
        sku: String?,
        price: Long,
        categoryId: UUID?,
        taxRateId: UUID?,
        imageUrl: String?,
        displayOrder: Int,
    ): ProductEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            ProductEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.description = description
                this.barcode = barcode
                this.sku = sku
                this.price = price
                this.categoryId = categoryId
                this.taxRateId = taxRateId
                this.imageUrl = imageUrl
                this.displayOrder = displayOrder
                this.isActive = true
            }
        productRepository.persist(entity)
        return entity
    }

    /**
     * IDで商品を取得する。
     */
    fun findById(id: UUID): ProductEntity? {
        tenantFilterService.enableFilter()
        return productRepository.findById(id)
    }

    /**
     * 複数IDで商品を一括取得する。
     */
    fun findByIds(ids: List<UUID>): List<ProductEntity> {
        tenantFilterService.enableFilter()
        return productRepository.findByIds(ids)
    }

    /**
     * バーコードで商品を検索する。
     */
    fun findByBarcode(barcode: String): ProductEntity? {
        tenantFilterService.enableFilter()
        return productRepository.findByBarcode(barcode)
    }

    /**
     * 商品一覧を検索する（ページネーション対応）。
     *
     * @return Pair<商品リスト, 総件数>
     */
    fun search(
        query: String?,
        categoryId: UUID?,
        activeOnly: Boolean,
        page: Int,
        pageSize: Int,
    ): Pair<List<ProductEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val products = productRepository.search(query, categoryId, activeOnly, panachePage)
        val totalCount = productRepository.searchCount(query, categoryId, activeOnly)
        return Pair(products, totalCount)
    }

    /**
     * 商品を更新する。
     */
    @Transactional
    fun update(
        id: UUID,
        name: String?,
        description: String?,
        barcode: String?,
        sku: String?,
        price: Long?,
        categoryId: UUID?,
        taxRateId: UUID?,
        imageUrl: String?,
        displayOrder: Int?,
        isActive: Boolean?,
    ): ProductEntity? {
        tenantFilterService.enableFilter()
        val entity = productRepository.findById(id) ?: return null

        name?.let { entity.name = it }
        description?.let { entity.description = it }
        barcode?.let { entity.barcode = it }
        sku?.let { entity.sku = it }
        price?.let { entity.price = it }
        if (categoryId != null) {
            entity.categoryId = categoryId
        }
        if (taxRateId != null) {
            entity.taxRateId = taxRateId
        }
        imageUrl?.let { entity.imageUrl = it }
        displayOrder?.let { entity.displayOrder = it }
        isActive?.let { entity.isActive = it }

        productRepository.persist(entity)
        return entity
    }

    /**
     * 商品をソフトデリートする（deleted_at を現在日時に設定し、is_active = false）。
     */
    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = productRepository.findById(id) ?: return false
        entity.isActive = false
        entity.deletedAt = java.time.Instant.now()
        productRepository.persist(entity)
        return true
    }
}
