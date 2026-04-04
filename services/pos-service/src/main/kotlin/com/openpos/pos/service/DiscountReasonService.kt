package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.repository.DiscountReasonRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * 値引き理由コードサービス。
 * 手動値引き時に選択する理由コードの CRUD を提供する。
 */
@ApplicationScoped
class DiscountReasonService {
    @Inject
    lateinit var discountReasonRepository: DiscountReasonRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listActive(): List<DiscountReasonEntity> {
        tenantFilterService.enableFilter()
        return discountReasonRepository.findActive()
    }

    fun listAll(): List<DiscountReasonEntity> {
        tenantFilterService.enableFilter()
        return discountReasonRepository.findAllOrdered()
    }

    @Transactional
    fun create(
        code: String,
        description: String,
    ): DiscountReasonEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            DiscountReasonEntity().apply {
                this.organizationId = orgId
                this.code = code
                this.description = description
            }
        discountReasonRepository.persist(entity)
        return entity
    }

    @Transactional
    fun update(
        id: UUID,
        description: String?,
        isActive: Boolean?,
    ): DiscountReasonEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        val entity = discountReasonRepository.find("id = ?1", id).firstResult() ?: return null
        description?.let { entity.description = it }
        isActive?.let { entity.isActive = it }
        discountReasonRepository.persist(entity)
        return entity
    }
}
