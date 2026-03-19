package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.SettlementEntity
import com.openpos.pos.repository.PaymentRepository
import com.openpos.pos.repository.SettlementRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class SettlementService {
    @Inject
    lateinit var settlementRepository: SettlementRepository

    @Inject
    lateinit var paymentRepository: PaymentRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun createSettlement(
        storeId: UUID,
        terminalId: UUID,
        staffId: UUID,
        cashActual: Long,
    ): SettlementEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()

        // Calculate expected cash from completed transactions' cash payments
        val cashExpected = paymentRepository.sumCashPaymentsByTerminal(storeId, terminalId, orgId)

        val settlement =
            SettlementEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.terminalId = terminalId
                this.staffId = staffId
                this.cashExpected = cashExpected
                this.cashActual = cashActual
                this.difference = cashActual - cashExpected
                this.settledAt = Instant.now()
            }
        settlementRepository.persist(settlement)
        return settlement
    }

    fun getSettlement(settlementId: UUID): SettlementEntity {
        tenantFilterService.enableFilter()
        return settlementRepository.findById(settlementId)
            ?: throw IllegalArgumentException("Settlement not found: $settlementId")
    }
}
