package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.GiftCardEntity
import com.openpos.pos.repository.GiftCardRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
class GiftCardService {
    @Inject
    lateinit var giftCardRepository: GiftCardRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun listAll(): List<GiftCardEntity> {
        tenantFilterService.enableFilter()
        return giftCardRepository.listAllOrdered()
    }

    fun findByCode(code: String): GiftCardEntity? {
        tenantFilterService.enableFilter()
        return giftCardRepository.findByCode(code)
    }

    @Transactional
    fun create(
        initialAmount: Long,
        expiresAt: Instant?,
    ): GiftCardEntity {
        require(initialAmount > 0) { "Initial amount must be positive" }
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        val entity =
            GiftCardEntity().apply {
                this.organizationId = orgId
                this.code = generateCode()
                this.initialAmount = initialAmount
                this.balance = initialAmount
                this.status = "PENDING"
                this.issuedAt = Instant.now()
                this.expiresAt = expiresAt
            }
        giftCardRepository.persist(entity)
        return entity
    }

    @Transactional
    fun activate(code: String): GiftCardEntity {
        tenantFilterService.enableFilter()
        val card =
            requireNotNull(giftCardRepository.findByCode(code)) { "Gift card not found: $code" }
        require(card.status == "PENDING") { "Gift card is not in PENDING status: ${card.status}" }
        card.status = "ACTIVE"
        giftCardRepository.persist(card)
        return card
    }

    @Transactional
    fun redeem(
        code: String,
        amount: Long,
    ): GiftCardEntity {
        require(amount > 0) { "Redeem amount must be positive" }
        tenantFilterService.enableFilter()
        val card =
            requireNotNull(giftCardRepository.findByCode(code)) { "Gift card not found: $code" }
        require(card.status == "ACTIVE") { "Gift card is not ACTIVE: ${card.status}" }
        require(card.balance >= amount) { "Insufficient balance: ${card.balance} < $amount" }
        card.balance -= amount
        if (card.balance == 0L) {
            card.status = "DEPLETED"
        }
        giftCardRepository.persist(card)
        return card
    }

    fun getBalance(code: String): GiftCardEntity {
        tenantFilterService.enableFilter()
        return requireNotNull(giftCardRepository.findByCode(code)) {
            "Gift card not found: $code"
        }
    }

    private fun generateCode(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return (1..16)
            .map { chars.random() }
            .chunked(4)
            .joinToString("-") { it.joinToString("") }
    }
}
