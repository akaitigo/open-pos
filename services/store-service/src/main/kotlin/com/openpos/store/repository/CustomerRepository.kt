package com.openpos.store.repository

import com.openpos.store.entity.CustomerEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.LockModeType
import java.util.UUID

@ApplicationScoped
class CustomerRepository : PanacheRepositoryBase<CustomerEntity, UUID> {
    fun listPaginated(page: Page): List<CustomerEntity> = findAll(Sort.ascending("name")).page(page).list()

    fun findByEmail(email: String): CustomerEntity? = find("email", email).firstResult()

    fun findAllByOrganizationId(organizationId: UUID): List<CustomerEntity> = find("organizationId = ?1", organizationId).list()

    fun findByIdForUpdate(id: UUID): CustomerEntity? = find("id = ?1", id).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()

    fun searchPaginated(
        search: String,
        page: Page,
    ): List<CustomerEntity> {
        val pattern = "%${search.lowercase()}%"
        return find(
            "lower(name) like ?1 or lower(email) like ?1 or phone like ?1",
            Sort.ascending("name"),
            pattern,
        ).page(page).list()
    }

    fun countBySearch(search: String): Long {
        val pattern = "%${search.lowercase()}%"
        return count("lower(name) like ?1 or lower(email) like ?1 or phone like ?1", pattern)
    }
}
