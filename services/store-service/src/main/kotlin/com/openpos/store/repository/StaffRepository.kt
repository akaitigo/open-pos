package com.openpos.store.repository

import com.openpos.store.entity.StaffEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StaffRepository : PanacheRepositoryBase<StaffEntity, UUID> {
    fun findByStoreId(
        storeId: UUID,
        page: Page,
    ): List<StaffEntity> = find("storeId = ?1", Sort.ascending("name"), storeId).page(page).list()

    fun countByStoreId(storeId: UUID): Long = count("storeId = ?1", storeId)

    fun findByEmail(email: String): StaffEntity? = find("email = ?1", email).firstResult()

    fun findAllByOrganizationId(organizationId: UUID): List<StaffEntity> = find("organizationId = ?1", organizationId).list()
}
