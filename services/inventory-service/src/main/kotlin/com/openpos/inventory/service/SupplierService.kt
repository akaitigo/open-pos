package com.openpos.inventory.service

import com.openpos.inventory.config.OrganizationIdHolder
import com.openpos.inventory.config.TenantFilterService
import com.openpos.inventory.entity.SupplierEntity
import com.openpos.inventory.repository.SupplierRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class SupplierService {
    @Inject lateinit var supplierRepository: SupplierRepository
    @Inject lateinit var tenantFilterService: TenantFilterService
    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(name: String, contactPerson: String?, email: String?, phone: String?, address: String?): SupplierEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity = SupplierEntity().apply {
            this.organizationId = orgId
            this.name = name
            this.contactPerson = contactPerson
            this.email = email
            this.phone = phone
            this.address = address
        }
        supplierRepository.persist(entity)
        return entity
    }

    fun findById(id: UUID): SupplierEntity? {
        tenantFilterService.enableFilter()
        return supplierRepository.findById(id)
    }

    fun list(page: Int, pageSize: Int): Pair<List<SupplierEntity>, Long> {
        tenantFilterService.enableFilter()
        val items = supplierRepository.listPaginated(Page.of(page, pageSize))
        val total = supplierRepository.count()
        return Pair(items, total)
    }

    @Transactional
    fun update(id: UUID, name: String?, contactPerson: String?, email: String?, phone: String?, address: String?): SupplierEntity? {
        tenantFilterService.enableFilter()
        val entity = supplierRepository.findById(id) ?: return null
        name?.let { entity.name = it }
        contactPerson?.let { entity.contactPerson = it }
        email?.let { entity.email = it }
        phone?.let { entity.phone = it }
        address?.let { entity.address = it }
        supplierRepository.persist(entity)
        return entity
    }
}
