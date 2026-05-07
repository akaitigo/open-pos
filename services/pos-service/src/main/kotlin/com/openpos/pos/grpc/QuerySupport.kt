package com.openpos.pos.grpc

import openpos.common.v1.PaginationResponse
import openpos.pos.v1.ListJournalEntriesRequest
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.TransactionStatus
import java.time.Instant
import java.util.UUID

internal data class TransactionListQuery(
    val storeId: UUID?,
    val terminalId: UUID?,
    val status: String?,
    val startDate: Instant?,
    val endDate: Instant?,
    val page: Int,
    val pageSize: Int,
)

internal data class JournalEntriesQuery(
    val type: String?,
    val startDate: Instant?,
    val endDate: Instant?,
    val page: Int,
    val pageSize: Int,
)

internal fun resolveTransactionListQuery(request: ListTransactionsRequest): TransactionListQuery {
    val pagination = request.resolvePaginationWindow()

    return TransactionListQuery(
        storeId = request.storeId.uuidOrNull(),
        terminalId = request.terminalId.uuidOrNull(),
        status =
            when (request.status) {
                TransactionStatus.TRANSACTION_STATUS_DRAFT -> "DRAFT"
                TransactionStatus.TRANSACTION_STATUS_COMPLETED -> "COMPLETED"
                TransactionStatus.TRANSACTION_STATUS_VOIDED -> "VOIDED"
                else -> null
            },
        startDate = request.resolveStartDate(),
        endDate = request.resolveEndDate(),
        page = pagination.page,
        pageSize = pagination.pageSize,
    )
}

internal fun resolveJournalEntriesQuery(request: ListJournalEntriesRequest): JournalEntriesQuery {
    val pagination = request.resolvePaginationWindow()

    return JournalEntriesQuery(
        type = request.type.ifBlank { null },
        startDate = request.resolveStartDate(),
        endDate = request.resolveEndDate(),
        page = pagination.page,
        pageSize = pagination.pageSize,
    )
}

internal fun buildPaginationResponse(
    page: Int,
    pageSize: Int,
    totalCount: Long,
): PaginationResponse {
    val totalPages = if (totalCount > 0) ((totalCount + pageSize - 1) / pageSize).toInt() else 0

    return PaginationResponse
        .newBuilder()
        .setPage(page + 1)
        .setPageSize(pageSize)
        .setTotalCount(totalCount)
        .setTotalPages(totalPages)
        .build()
}

private data class PaginationWindow(
    val page: Int,
    val pageSize: Int,
)

private fun ListTransactionsRequest.resolvePaginationWindow(): PaginationWindow =
    PaginationWindow(
        page = if (hasPagination()) pagination.page - 1 else 0,
        pageSize = if (hasPagination() && pagination.pageSize > 0) pagination.pageSize.coerceAtMost(100) else 20,
    )

private fun ListJournalEntriesRequest.resolvePaginationWindow(): PaginationWindow =
    PaginationWindow(
        page = if (hasPagination()) pagination.page - 1 else 0,
        pageSize = if (hasPagination() && pagination.pageSize > 0) pagination.pageSize.coerceAtMost(100) else 20,
    )

private fun ListTransactionsRequest.resolveStartDate(): Instant? =
    if (hasDateRange() && dateRange.start.isNotBlank()) {
        Instant.parse(dateRange.start)
    } else {
        null
    }

private fun ListTransactionsRequest.resolveEndDate(): Instant? =
    if (hasDateRange() && dateRange.end.isNotBlank()) {
        Instant.parse(dateRange.end)
    } else {
        null
    }

private fun ListJournalEntriesRequest.resolveStartDate(): Instant? =
    if (hasDateRange() && dateRange.start.isNotBlank()) {
        Instant.parse(dateRange.start)
    } else {
        null
    }

private fun ListJournalEntriesRequest.resolveEndDate(): Instant? =
    if (hasDateRange() && dateRange.end.isNotBlank()) {
        Instant.parse(dateRange.end)
    } else {
        null
    }
