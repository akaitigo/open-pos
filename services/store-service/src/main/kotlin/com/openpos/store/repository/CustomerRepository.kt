package com.openpos.store.repository

import com.openpos.store.entity.CustomerEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class CustomerRepository : PanacheRepositoryBase<CustomerEntity, UUID> {
    fun listPaginated(page: Page): List<CustomerEntity> = findAll(Sort.ascending("name")).page(page).list()

    fun findByEmail(email: String): CustomerEntity? = find("email", email).firstResult()

    fun findAllByOrganizationId(organizationId: UUID): List<CustomerEntity> = find("organizationId = ?1", organizationId).list()
}
