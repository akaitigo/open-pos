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

    fun findByOrganizationIdAndStaffId(
        organizationId: UUID,
        staffId: UUID,
        page: Page,
    ): List<AuditLogEntity> =
        find(
            "organizationId = ?1 AND staffId = ?2",
            Sort.descending("createdAt"),
            organizationId,
            staffId,
        ).page(page).list()

    fun countByOrganizationIdAndStaffId(
        organizationId: UUID,
        staffId: UUID,
    ): Long = count("organizationId = ?1 AND staffId = ?2", organizationId, staffId)

    fun findByOrganizationIdAndAction(
        organizationId: UUID,
        action: String,
        page: Page,
    ): List<AuditLogEntity> =
        find(
            "organizationId = ?1 AND action = ?2",
            Sort.descending("createdAt"),
            organizationId,
            action,
        ).page(page).list()

    fun countByOrganizationIdAndAction(
        organizationId: UUID,
        action: String,
    ): Long = count("organizationId = ?1 AND action = ?2", organizationId, action)
}
