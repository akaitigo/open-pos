package com.openpos.product.repository

import com.openpos.product.entity.ProductBundleItemEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProductBundleItemRepository : PanacheRepositoryBase<ProductBundleItemEntity, UUID> {
    fun listByBundleId(bundleId: UUID): List<ProductBundleItemEntity> =
        find("bundleId", bundleId).list()
}
