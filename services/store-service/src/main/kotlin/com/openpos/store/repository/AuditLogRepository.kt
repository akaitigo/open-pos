package com.openpos.store.repository

import com.openpos.store.entity.AuditLogEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class AuditLogRepository : PanacheRepositoryBase<AuditLogEntity, UUID> {
    fun findByOrganizationId(
        organizationId: UUID,
        page: Page,
    ): List<AuditLogEntity> =
        find("organizationId = ?1", Sort.descending("createdAt"), organizationId)
            .page(page)
            .list()

    fun countByOrganizationId(organizationId: UUID): Long = count("organizationId = ?1", organizationId)
}
