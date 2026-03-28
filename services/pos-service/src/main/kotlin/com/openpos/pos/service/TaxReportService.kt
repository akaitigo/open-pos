package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.repository.TaxSummaryRepository
import com.openpos.pos.repository.TransactionRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class TaxReportService {
    @Inject
    lateinit var transactionRepository: TransactionRepository

    @Inject
    lateinit var taxSummaryRepository: TaxSummaryRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun getTaxReport(
        storeId: UUID,
        startDate: Instant,
        endDate: Instant,
    ): List<TaxReportItem> {
        requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        tenantFilterService.enableFilter()

        val completedTransactionIds =
            transactionRepository.findCompletedTransactionIds(storeId, startDate, endDate)

        if (completedTransactionIds.isEmpty()) {
            return emptyList()
        }

        val taxSummaries = taxSummaryRepository.findByTransactionIds(completedTransactionIds)

        return taxSummaries
            .groupBy { it.taxRateName }
            .map { (taxRateName, summaries) ->
                val first = summaries.first()
                val uniqueTransactionCount = summaries.map { it.transactionId }.distinct().size
                TaxReportItem(
                    taxRateName = taxRateName,
                    taxRatePercentage = first.taxRate,
                    isReduced = first.isReduced,
                    taxableAmount = summaries.sumOf { it.taxableAmount },
                    taxAmount = summaries.sumOf { it.taxAmount },
                    transactionCount = uniqueTransactionCount,
                )
            }.sortedWith(
                compareByDescending<TaxReportItem> { it.isReduced.not() }.thenBy { it.taxRateName },
            )
    }
}

data class TaxReportItem(
    val taxRateName: String,
    val taxRatePercentage: String,
    val isReduced: Boolean,
    val taxableAmount: Long,
    val taxAmount: Long,
    val transactionCount: Int,
)
