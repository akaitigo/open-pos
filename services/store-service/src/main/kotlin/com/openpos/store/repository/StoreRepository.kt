package com.openpos.store.repository

import com.openpos.store.entity.StoreEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StoreRepository : PanacheRepositoryBase<StoreEntity, UUID> {
    fun listPaginated(page: Page): List<StoreEntity> = findAll(Sort.ascending("name")).page(page).list()

    fun findAllByOrganizationId(organizationId: UUID): List<StoreEntity> = find("organizationId = ?1", organizationId).list()
}
