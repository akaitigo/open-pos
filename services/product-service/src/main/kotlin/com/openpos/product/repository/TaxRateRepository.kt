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

    /**
     * 組織内のデフォルト税率を取得する。
     */
    fun findDefaultsByOrganizationId(organizationId: UUID): List<TaxRateEntity> =
        list("organizationId = ?1 AND isDefault = true", organizationId)

    /**
     * 指定 ID を除く組織内のデフォルト税率を取得する。
     */
    fun findDefaultsByOrganizationIdExcludingId(
        organizationId: UUID,
        excludedId: UUID,
    ): List<TaxRateEntity> = list("organizationId = ?1 AND isDefault = true AND id <> ?2", organizationId, excludedId)
}
