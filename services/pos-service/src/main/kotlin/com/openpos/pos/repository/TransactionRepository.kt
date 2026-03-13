package com.openpos.pos.repository

import com.openpos.pos.entity.TransactionEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class TransactionRepository : PanacheRepositoryBase<TransactionEntity, UUID> {
    fun findByClientId(clientId: String): TransactionEntity? = find("clientId = ?1", clientId).firstResult()

    fun listByStoreId(
        storeId: UUID,
        page: Page,
    ): List<TransactionEntity> =
        find("storeId = ?1", Sort.descending("createdAt"), storeId)
            .page(page)
            .list()

    fun listByFilters(
        storeId: UUID?,
        terminalId: UUID?,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
        page: Page,
    ): List<TransactionEntity> {
        val (query, params) = buildFilterQuery(storeId, terminalId, status, startDate, endDate)
        return find(query, Sort.descending("createdAt"), params)
            .page(page)
            .list()
    }

    fun countByFilters(
        storeId: UUID?,
        terminalId: UUID?,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Long {
        val (query, params) = buildFilterQuery(storeId, terminalId, status, startDate, endDate)
        return count(query, params)
    }

    private fun buildFilterQuery(
        storeId: UUID?,
        terminalId: UUID?,
        status: String?,
        startDate: Instant?,
        endDate: Instant?,
    ): Pair<String, Map<String, Any>> {
        val conditions = mutableListOf<String>()
        val params = mutableMapOf<String, Any>()
        if (storeId != null) {
            conditions.add("storeId = :storeId")
            params["storeId"] = storeId
        }
        if (terminalId != null) {
            conditions.add("terminalId = :terminalId")
            params["terminalId"] = terminalId
        }
        if (!status.isNullOrBlank()) {
            conditions.add("status = :status")
            params["status"] = status
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
