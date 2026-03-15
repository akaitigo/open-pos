package com.openpos.inventory.repository

import com.openpos.inventory.entity.SupplierEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SupplierRepository : PanacheRepositoryBase<SupplierEntity, UUID> {
    fun listPaginated(page: Page): List<SupplierEntity> = findAll(Sort.ascending("name")).page(page).list()
}
