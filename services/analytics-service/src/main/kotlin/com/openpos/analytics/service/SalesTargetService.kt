package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.SalesTargetEntity
import com.openpos.analytics.repository.SalesTargetRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.LocalDate
import java.util.UUID

/**
 * 売上目標サービス。
 * 月次の売上目標の設定と進捗確認を提供する。
 */
@ApplicationScoped
class SalesTargetService {
    @Inject
    lateinit var salesTargetRepository: SalesTargetRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listAll(): List<SalesTargetEntity> {
        tenantFilterService.enableFilter()
        return salesTargetRepository.listByOrganization()
    }

    fun findByStoreAndMonth(
        storeId: UUID?,
        targetMonth: LocalDate,
    ): SalesTargetEntity? {
        tenantFilterService.enableFilter()
        return salesTargetRepository.findByStoreAndMonth(storeId, targetMonth)
    }

    @Transactional
    fun upsert(
        storeId: UUID?,
        targetMonth: LocalDate,
        targetAmount: Long,
    ): SalesTargetEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        tenantFilterService.enableFilter()
        val existing = salesTargetRepository.findByStoreAndMonth(storeId, targetMonth)
        if (existing != null) {
            existing.targetAmount = targetAmount
            salesTargetRepository.persist(existing)
            return existing
        }

        val entity =
            SalesTargetEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.targetMonth = targetMonth
                this.targetAmount = targetAmount
            }
        salesTargetRepository.persist(entity)
        return entity
    }
}
