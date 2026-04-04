package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.ProductVariantEntity
import com.openpos.product.repository.ProductRepository
import com.openpos.product.repository.ProductVariantRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional

@ApplicationScoped
class ProductVariantService {
    @Inject
    lateinit var variantRepository: ProductVariantRepository

    @Inject
    lateinit var productRepository: ProductRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listByProductId(productId: java.util.UUID): List<ProductVariantEntity> {
        tenantFilterService.enableFilter()
        return variantRepository.findByProductId(productId)
    }

    fun listByProductIds(productIds: List<java.util.UUID>): List<ProductVariantEntity> {
        tenantFilterService.enableFilter()
        return variantRepository.findByProductIds(productIds)
    }

    /**
     * バーコードでバリアントを検索する。
     */
    fun findByBarcode(barcode: String): ProductVariantEntity? {
        tenantFilterService.enableFilter()
        return variantRepository.findByBarcode(barcode)
    }

    @Transactional
    fun create(
        productId: java.util.UUID,
        name: String,
        sku: String?,
        barcode: String?,
        price: Long,
        displayOrder: Int,
    ): ProductVariantEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        // テナント検証: Hibernate Filter 有効状態で親商品を取得し、
        // 他テナントの productId が指定された場合は null になるため NOT_FOUND として拒否する
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        tenantFilterService.enableFilter()
        requireNotNull(productRepository.find("id = ?1", productId).firstResult()) {
            "Product not found or belongs to another tenant: $productId"
        }

        val entity =
            ProductVariantEntity().apply {
                this.organizationId = orgId
                this.productId = productId
                this.name = name
                this.sku = sku
                this.barcode = barcode
                this.price = price
                this.displayOrder = displayOrder
            }
        variantRepository.persist(entity)
        return entity
    }

    @Transactional
    fun update(
        id: java.util.UUID,
        name: String?,
        sku: String?,
        barcode: String?,
        price: Long?,
        isActive: Boolean?,
        displayOrder: Int?,
    ): ProductVariantEntity {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        val entity = requireNotNull(variantRepository.find("id = ?1", id).firstResult()) { "Variant not found: $id" }
        name?.let { entity.name = it }
        sku?.let { entity.sku = it }
        barcode?.let { entity.barcode = it }
        price?.let { entity.price = it }
        isActive?.let { entity.isActive = it }
        displayOrder?.let { entity.displayOrder = it }
        variantRepository.persist(entity)
        return entity
    }

    @Transactional
    fun delete(id: java.util.UUID): Boolean {
        tenantFilterService.enableFilter()
        // deleteById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        val entity = variantRepository.find("id = ?1", id).firstResult() ?: return false
        variantRepository.delete(entity)
        return true
    }
}
