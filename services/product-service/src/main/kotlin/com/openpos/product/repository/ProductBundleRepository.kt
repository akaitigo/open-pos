package com.openpos.product.repository

import com.openpos.product.entity.ProductBundleEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProductBundleRepository : PanacheRepositoryBase<ProductBundleEntity, UUID>
