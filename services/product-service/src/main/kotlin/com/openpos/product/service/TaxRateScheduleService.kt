package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.TaxRateScheduleEntity
import com.openpos.product.repository.TaxRateRepository
import com.openpos.product.repository.TaxRateScheduleRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.math.BigDecimal
import java.time.LocalDate
import java.util.UUID

/**
 * 税率変更スケジュールサービス。
 * 指定日に自動で税率を変更するスケジュールの管理と適用を提供する。
 */
@ApplicationScoped
class TaxRateScheduleService {
    @Inject
    lateinit var scheduleRepository: TaxRateScheduleRepository

    @Inject
    lateinit var taxRateRepository: TaxRateRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    companion object {
        private val logger: Logger = Logger.getLogger(TaxRateScheduleService::class::class.java)
    }

    fun listByTaxRateId(taxRateId: UUID): List<TaxRateScheduleEntity> {
        tenantFilterService.enableFilter()
        return scheduleRepository.findByTaxRateId(taxRateId)
    }

    @Transactional
    fun create(
        taxRateId: UUID,
        newRate: BigDecimal,
        effectiveDate: LocalDate,
    ): TaxRateScheduleEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            TaxRateScheduleEntity().apply {
                this.organizationId = orgId
                this.taxRateId = taxRateId
                this.newRate = newRate
                this.effectiveDate = effectiveDate
            }
        scheduleRepository.persist(entity)
        logger.info("Created tax rate schedule: taxRate=$taxRateId, newRate=$newRate, effectiveDate=$effectiveDate")
        return entity
    }

    /**
     * 保留中のスケジュールを適用する（日次バッチ用）。
     * effectiveDate <= today かつ applied = false のスケジュールを処理する。
     */
    @Transactional
    fun applyPendingSchedules(): Int {
        val today = LocalDate.now()
        val pending = scheduleRepository.findPendingByDate(today)
        var applied = 0

        for (schedule in pending) {
            // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
            val taxRate = taxRateRepository.find("id = ?1", schedule.taxRateId).firstResult()
            if (taxRate != null) {
                taxRate.rate = schedule.newRate
                taxRateRepository.persist(taxRate)
                schedule.applied = true
                scheduleRepository.persist(schedule)
                applied++
                logger.info(
                    "Applied tax rate schedule: taxRate=${schedule.taxRateId}, " +
                        "newRate=${schedule.newRate}, effectiveDate=${schedule.effectiveDate}",
                )
            }
        }
        return applied
    }
}
