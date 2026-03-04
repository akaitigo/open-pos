package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.TerminalEntity
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
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun register(
        storeId: UUID,
        terminalCode: String,
        name: String,
    ): TerminalEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            TerminalEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalCode = terminalCode
                this.name = name
                this.isActive = true
            }
        terminalRepository.persist(entity)
        return entity
    }

    fun listByStoreId(storeId: UUID): List<TerminalEntity> {
        tenantFilterService.enableFilter()
        return terminalRepository.findByStoreId(storeId)
    }

    @Transactional
    fun updateSync(terminalId: UUID): TerminalEntity? {
        tenantFilterService.enableFilter()
        val entity = terminalRepository.findById(terminalId) ?: return null
        entity.lastSyncAt = Instant.now()
        terminalRepository.persist(entity)
        return entity
    }
}
