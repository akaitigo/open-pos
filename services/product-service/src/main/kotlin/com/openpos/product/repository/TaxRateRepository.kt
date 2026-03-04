package com.openpos.product.repository

import com.openpos.product.entity.TaxRateEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

/**
 * 税率リポジトリ。
 */
@ApplicationScoped
class TaxRateRepository : PanacheRepositoryBase<TaxRateEntity, UUID> {
    /**
     * 有効な税率のみ取得する。
     */
    fun findActive(): List<TaxRateEntity> = list("isActive = true ORDER BY name ASC")
}
