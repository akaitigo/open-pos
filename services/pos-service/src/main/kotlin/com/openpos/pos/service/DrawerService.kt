package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.DrawerEntity
import com.openpos.pos.repository.DrawerRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class DrawerService {
    @Inject
    lateinit var drawerRepository: DrawerRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun openDrawer(
        storeId: UUID,
        terminalId: UUID,
        openingAmount: Long,
    ): DrawerEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()

        val existing = drawerRepository.findByTerminalForUpdate(storeId, terminalId)
        require(existing == null) { "Drawer is already open for this terminal" }

        val drawer =
            DrawerEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalId = terminalId
                this.openingAmount = openingAmount
                this.currentAmount = openingAmount
                this.isOpen = true
                this.openedAt = Instant.now()
            }
        drawerRepository.persist(drawer)
        return drawer
    }

    @Transactional
    fun closeDrawer(
        storeId: UUID,
        terminalId: UUID,
    ): DrawerEntity {
        tenantFilterService.enableFilter()
        val drawer =
            drawerRepository.findByTerminal(storeId, terminalId)
                ?: throw IllegalArgumentException("No open drawer found for this terminal")

        drawer.isOpen = false
        drawer.closedAt = Instant.now()
        drawerRepository.persist(drawer)
        return drawer
    }

    fun getDrawerStatus(
        storeId: UUID,
        terminalId: UUID,
    ): DrawerEntity {
        tenantFilterService.enableFilter()
        return drawerRepository.findLatestByTerminal(storeId, terminalId)
            ?: throw IllegalArgumentException("No drawer found for this terminal")
    }
}
