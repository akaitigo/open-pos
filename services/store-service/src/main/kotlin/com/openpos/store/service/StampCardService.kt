package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StampCardEntity
import com.openpos.store.repository.StampCardRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class StampCardService {
    @Inject
    lateinit var stampCardRepository: StampCardRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun getByCustomerId(customerId: UUID): StampCardEntity? {
        tenantFilterService.enableFilter()
        return stampCardRepository.findByCustomerId(customerId)
    }

    @Transactional
    fun issue(
        customerId: UUID,
        maxStamps: Int,
        rewardDescription: String?,
    ): StampCardEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val effectiveMaxStamps = if (maxStamps > 0) maxStamps else 10

        val entity =
            StampCardEntity().apply {
                this.organizationId = orgId
                this.customerId = customerId
                this.stampCount = 0
                this.maxStamps = effectiveMaxStamps
                this.rewardDescription = rewardDescription
                this.status = "ACTIVE"
                this.issuedAt = Instant.now()
            }
        stampCardRepository.persist(entity)
        return entity
    }

    @Transactional
    fun addStamp(customerId: UUID): StampCardEntity {
        tenantFilterService.enableFilter()
        val card =
            requireNotNull(stampCardRepository.findActiveByCustomerId(customerId)) {
                "Active stamp card not found for customer: $customerId"
            }
        require(card.status == "ACTIVE") { "Stamp card is not ACTIVE: ${card.status}" }
        card.stampCount += 1
        if (card.stampCount >= card.maxStamps) {
            card.status = "COMPLETED"
        }
        stampCardRepository.persist(card)
        return card
    }

    @Transactional
    fun redeemReward(customerId: UUID): StampCardEntity {
        tenantFilterService.enableFilter()
        val card =
            requireNotNull(stampCardRepository.findByCustomerId(customerId)) {
                "Stamp card not found for customer: $customerId"
            }
        require(card.status == "COMPLETED") {
            "Stamp card is not COMPLETED (stamps: ${card.stampCount}/${card.maxStamps})"
        }
        card.stampCount = 0
        card.status = "ACTIVE"
        stampCardRepository.persist(card)
        return card
    }
}
