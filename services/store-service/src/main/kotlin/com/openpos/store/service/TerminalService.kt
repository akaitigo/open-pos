package com.openpos.store.service

import com.openpos.store.cache.StoreCacheService
import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.TerminalEntity
import com.openpos.store.repository.StoreRepository
import com.openpos.store.repository.TerminalRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class TerminalService {
    @Inject
    lateinit var terminalRepository: TerminalRepository

    @Inject
    lateinit var storeRepository: StoreRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var cacheService: StoreCacheService

    @Transactional
    fun register(
        storeId: UUID,
        terminalCode: String,
        name: String,
    ): TerminalEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        // storeId が自組織に属するか検証
        tenantFilterService.enableFilter()
        val store =
            storeRepository.findById(storeId)
                ?: throw IllegalArgumentException("Store $storeId not found in organization $orgId")
        require(store.organizationId == orgId) {
            "Store $storeId does not belong to organization $orgId"
        }
        val entity =
            TerminalEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalCode = terminalCode
                this.name = name
                this.isActive = true
            }
        terminalRepository.persist(entity)
        cacheService.invalidateTerminalList(orgId.toString(), storeId.toString())
        return entity
    }

    fun listByStoreId(storeId: UUID): List<TerminalEntity> {
        tenantFilterService.enableFilter()
        return terminalRepository.findByStoreId(storeId)
    }

    @Transactional
    fun updateSync(terminalId: UUID): TerminalEntity? {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()
        val entity = terminalRepository.findById(terminalId) ?: return null
        entity.lastSyncAt = Instant.now()
        terminalRepository.persist(entity)
        cacheService.invalidateTerminalList(orgId.toString(), entity.storeId.toString())
        return entity
    }
}
