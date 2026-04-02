package com.openpos.product.repository

import com.openpos.product.entity.ProductVariantEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProductVariantRepository : PanacheRepositoryBase<ProductVariantEntity, UUID> {
    fun findByProductId(productId: UUID): List<ProductVariantEntity> = find("productId = ?1 order by displayOrder asc", productId).list()

    fun findByProductIds(productIds: List<UUID>): List<ProductVariantEntity> {
        if (productIds.isEmpty()) return emptyList()
        return find("productId in ?1 order by displayOrder asc", productIds).list()
    }

    fun findByBarcode(barcode: String): ProductVariantEntity? = find("barcode = ?1", barcode).firstResult()
}
