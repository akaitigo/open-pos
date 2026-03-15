package com.openpos.pos.repository

import com.openpos.pos.entity.JournalEntryEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class JournalEntryRepository : PanacheRepositoryBase<JournalEntryEntity, UUID> {
    fun listByFilters(
        type: String?,
        startDate: Instant?,
        endDate: Instant?,
        page: Page,
    ): List<JournalEntryEntity> {
        val (query, params) = buildFilterQuery(type, startDate, endDate)
        return find(query, Sort.descending("createdAt"), params)
            .page(page)
            .list()
    }

    fun countByFilters(
        type: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Long {
        val (query, params) = buildFilterQuery(type, startDate, endDate)
        return count(query, params)
    }

    private fun buildFilterQuery(
        type: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Pair<String, Map<String, Any>> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()
        if (!type.isNullOrBlank()) {
            conditions.add("type = :type")
            params["type"] = type
        }
        if (startDate != null) {
            conditions.add("createdAt >= :startDate")
            params["startDate"] = startDate
        }
        if (endDate != null) {
            conditions.add("createdAt <= :endDate")
            params["endDate"] = endDate
        }
        val query = if (conditions.isEmpty()) "1 = 1" else conditions.joinToString(" and ")
        return Pair(query, params)
    }
}
