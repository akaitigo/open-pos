package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.GiftCardEntity
import com.openpos.store.repository.GiftCardRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@ApplicationScoped
class GiftCardService {
    @Inject lateinit var giftCardRepository: GiftCardRepository

    @Inject lateinit var tenantFilterService: TenantFilterService

    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun activate(
        code: String,
        balance: Long,
    ): GiftCardEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity =
            GiftCardEntity().apply {
                this.organizationId = orgId
                this.code = code
                this.balance = balance
                this.initialBalance = balance
                this.status = "ACTIVE"
            }
        giftCardRepository.persist(entity)
        return entity
    }

    fun findByCode(code: String): GiftCardEntity? {
        tenantFilterService.enableFilter()
        return giftCardRepository.findByCode(code)
    }

    fun findById(id: UUID): GiftCardEntity? {
        tenantFilterService.enableFilter()
        return giftCardRepository.findById(id)
    }

    @Transactional
    fun charge(
        code: String,
        amount: Long,
    ): GiftCardEntity? {
        tenantFilterService.enableFilter()
        val entity = giftCardRepository.findByCode(code) ?: return null
        if (entity.status != "ACTIVE") return null
        entity.balance += amount
        giftCardRepository.persist(entity)
        return entity
    }

    @Transactional
    fun redeem(
        code: String,
        amount: Long,
    ): GiftCardEntity? {
        tenantFilterService.enableFilter()
        val entity = giftCardRepository.findByCodeForUpdate(code) ?: return null
        if (entity.status != "ACTIVE" || entity.balance < amount) return null
        entity.balance -= amount
        if (entity.balance <= 0) entity.status = "USED"
        giftCardRepository.persist(entity)
        return entity
    }
}
