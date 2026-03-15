package com.openpos.product.repository

import com.openpos.product.entity.TaxRateScheduleEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class TaxRateScheduleRepository : PanacheRepositoryBase<TaxRateScheduleEntity, UUID> {
    fun findPendingByDate(date: LocalDate): List<TaxRateScheduleEntity> =
        list("effectiveDate <= ?1 AND applied = false", Sort.ascending("effectiveDate"), date)

    fun findByTaxRateId(taxRateId: UUID): List<TaxRateScheduleEntity> = list("taxRateId = ?1", Sort.ascending("effectiveDate"), taxRateId)
}
