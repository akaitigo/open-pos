package com.openpos.pos.grpc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import openpos.common.v1.DateRange
import openpos.common.v1.PaginationRequest
import openpos.pos.v1.ListJournalEntriesRequest
import openpos.pos.v1.ListTransactionsRequest
import openpos.pos.v1.TransactionStatus
import java.time.Instant
import java.util.UUID

class QuerySupportTest {
    private val storeId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val terminalId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `resolves transaction list query filters and pagination`() {
        val query =
            resolveTransactionListQuery(
                ListTransactionsRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setTerminalId(terminalId.toString())
                    .setStatus(TransactionStatus.TRANSACTION_STATUS_COMPLETED)
                    .setPagination(
                        PaginationRequest
                            .newBuilder()
                            .setPage(3)
                            .setPageSize(200)
                            .build(),
                    ).setDateRange(
                        DateRange
                            .newBuilder()
                            .setStart("2026-05-01T00:00:00Z")
                            .setEnd("2026-05-02T00:00:00Z")
                            .build(),
                    ).build(),
            )

        assertEquals(storeId, query.storeId)
        assertEquals(terminalId, query.terminalId)
        assertEquals("COMPLETED", query.status)
        assertEquals(2, query.page)
        assertEquals(100, query.pageSize)
        assertEquals(Instant.parse("2026-05-01T00:00:00Z"), query.startDate)
        assertEquals(Instant.parse("2026-05-02T00:00:00Z"), query.endDate)
    }

    @Test
    fun `uses default transaction list pagination and optional filters`() {
        val query = resolveTransactionListQuery(ListTransactionsRequest.getDefaultInstance())

        assertNull(query.storeId)
        assertNull(query.terminalId)
        assertNull(query.status)
        assertNull(query.startDate)
        assertNull(query.endDate)
        assertEquals(0, query.page)
        assertEquals(20, query.pageSize)
    }

    @Test
    fun `resolves journal entry query filters and pagination`() {
        val query =
            resolveJournalEntriesQuery(
                ListJournalEntriesRequest
                    .newBuilder()
                    .setType("SALE")
                    .setPagination(
                        PaginationRequest
                            .newBuilder()
                            .setPage(2)
                            .setPageSize(50)
                            .build(),
                    ).setDateRange(
                        DateRange
                            .newBuilder()
                            .setStart("2026-05-03T00:00:00Z")
                            .setEnd("2026-05-04T00:00:00Z")
                            .build(),
                    ).build(),
            )

        assertEquals("SALE", query.type)
        assertEquals(1, query.page)
        assertEquals(50, query.pageSize)
        assertEquals(Instant.parse("2026-05-03T00:00:00Z"), query.startDate)
        assertEquals(Instant.parse("2026-05-04T00:00:00Z"), query.endDate)
    }

    @Test
    fun `builds pagination response with total pages`() {
        val response = buildPaginationResponse(page = 1, pageSize = 20, totalCount = 45)

        assertEquals(2, response.page)
        assertEquals(20, response.pageSize)
        assertEquals(45, response.totalCount)
        assertEquals(3, response.totalPages)
    }
}
