package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.repository.TaxRateRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.math.BigDecimal
import java.util.UUID

/**
 * 税率のビジネスロジック層。
 * CRUD とデフォルト税率管理を提供する。
 */
@ApplicationScoped
class TaxRateService {
    @Inject
    lateinit var taxRateRepository: TaxRateRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * 税率を作成する。
     */
    @Transactional
    fun create(
        name: String,
        rate: BigDecimal,
        taxType: String,
        isDefault: Boolean,
    ): TaxRateEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        if (isDefault) {
            clearExistingDefaults(orgId)
        }

        val entity =
            TaxRateEntity().apply {
                this.organizationId = orgId
                this.name = name
                this.rate = rate
                this.taxType = taxType
                this.isDefault = isDefault
                this.isActive = true
            }
        taxRateRepository.persist(entity)
        return entity
    }

    /**
     * 有効な税率一覧を取得する。
     */
    fun listAll(): List<TaxRateEntity> {
        tenantFilterService.enableFilter()
        return taxRateRepository.findActive()
    }

    /**
     * IDで税率を取得する。
     */
    fun findById(id: UUID): TaxRateEntity? {
        tenantFilterService.enableFilter()
        return taxRateRepository.findById(id)
    }

    /**
     * 税率を削除する（論理削除: isActive = false）。
     */
    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = taxRateRepository.findById(id) ?: return false
        entity.isActive = false
        taxRateRepository.persist(entity)
        return true
    }

    /**
     * 税率を更新する。
     */
    @Transactional
    fun update(
        id: UUID,
        name: String?,
        rate: BigDecimal?,
        taxType: String?,
        isActive: Boolean?,
        isDefault: Boolean?,
    ): TaxRateEntity? {
        tenantFilterService.enableFilter()
        val entity = taxRateRepository.findById(id) ?: return null

        name?.let { entity.name = it }
        rate?.let { entity.rate = it }
        taxType?.let { entity.taxType = it }
        isActive?.let { entity.isActive = it }
        isDefault?.let {
            if (it) {
                clearExistingDefaults(entity.organizationId, entity.id)
            }
            entity.isDefault = it
        }

        taxRateRepository.persist(entity)
        return entity
    }

    private fun clearExistingDefaults(
        organizationId: UUID,
        excludedId: UUID? = null,
    ) {
        val defaults =
            if (excludedId == null) {
                taxRateRepository.findDefaultsByOrganizationId(organizationId)
            } else {
                taxRateRepository.findDefaultsByOrganizationIdExcludingId(organizationId, excludedId)
            }

        defaults.forEach { it.isDefault = false }
    }
}
