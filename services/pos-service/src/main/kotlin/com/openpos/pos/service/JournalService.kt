package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.JournalEntryEntity
import com.openpos.pos.repository.JournalEntryRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 電子ジャーナルサービス。
 * 取引操作の不変ログを記録・検索する。
 */
@ApplicationScoped
class JournalService {
    @Inject
    lateinit var journalEntryRepository: JournalEntryRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun recordEntry(
        type: String,
        transactionId: UUID?,
        staffId: UUID,
        terminalId: UUID,
        details: String,
    ): JournalEntryEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }

        val entity =
            JournalEntryEntity().apply {
                this.organizationId = orgId
                this.type = type
                this.transactionId = transactionId
                this.staffId = staffId
                this.terminalId = terminalId
                this.details = details
            }
        journalEntryRepository.persist(entity)
        return entity
    }

    fun listEntries(
        type: String?,
        startDate: Instant?,
        endDate: Instant?,
        page: Int,
        pageSize: Int,
    ): Pair<List<JournalEntryEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        val entries = journalEntryRepository.listByFilters(type, startDate, endDate, panachePage)
        val totalCount = journalEntryRepository.countByFilters(type, startDate, endDate)
        return Pair(entries, totalCount)
    }
}
