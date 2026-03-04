package com.openpos.store.repository

import com.openpos.store.entity.OrganizationEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class OrganizationRepository : PanacheRepositoryBase<OrganizationEntity, UUID> {
    fun findByIdNotDeleted(id: UUID): OrganizationEntity? = find("id = ?1 AND deletedAt IS NULL", id).firstResult()
}
