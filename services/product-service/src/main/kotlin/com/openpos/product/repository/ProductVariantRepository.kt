package com.openpos.product.repository

import com.openpos.product.entity.ProductVariantEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProductVariantRepository : PanacheRepositoryBase<ProductVariantEntity, UUID> {
    fun listByProductId(productId: UUID): List<ProductVariantEntity> =
        find("productId", productId).list()
}
