package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StoreEntity
import com.openpos.store.repository.StoreRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class StoreService {
    @Inject
    lateinit var storeRepository: StoreRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(
        name: String,
        address: String?,
        phone: String?,
        timezone: String,
        settings: String,
    ): StoreEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            StoreEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.address = address
                this.phone = phone
                this.timezone = timezone
                this.settings = settings
                this.isActive = true
            }
        storeRepository.persist(entity)
        return entity
    }

    fun findById(id: UUID): StoreEntity? {
        tenantFilterService.enableFilter()
        return storeRepository.findById(id)
    }

    fun list(
        page: Int,
        pageSize: Int,
    ): Pair<List<StoreEntity>, Long> {
        tenantFilterService.enableFilter()
        val stores = storeRepository.listPaginated(Page.of(page, pageSize))
        val totalCount = storeRepository.count()
        return Pair(stores, totalCount)
    }

    @Transactional
    fun update(
        id: UUID,
        name: String?,
        address: String?,
        phone: String?,
        timezone: String?,
        settings: String?,
        isActive: Boolean?,
    ): StoreEntity? {
        tenantFilterService.enableFilter()
        val entity = storeRepository.findById(id) ?: return null
        name?.let { entity.name = it }
        address?.let { entity.address = it }
        phone?.let { entity.phone = it }
        timezone?.let { entity.timezone = it }
        settings?.let { entity.settings = it }
        isActive?.let { entity.isActive = it }
        storeRepository.persist(entity)
        return entity
    }
}
