package com.openpos.analytics.repository

import com.openpos.analytics.entity.ProductAlertEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class ProductAlertRepository : PanacheRepositoryBase<ProductAlertEntity, UUID> {
    fun findByOrganizationId(
        organizationId: UUID,
        page: Page,
    ): List<ProductAlertEntity> =
        find("organizationId = ?1", Sort.descending("createdAt"), organizationId)
            .page(page)
            .list()

    fun countByOrganizationId(organizationId: UUID): Long = count("organizationId = ?1", organizationId)

    fun findUnreadByOrganizationId(organizationId: UUID): List<ProductAlertEntity> =
        list("organizationId = ?1 AND isRead = false", Sort.descending("createdAt"), organizationId)
}
