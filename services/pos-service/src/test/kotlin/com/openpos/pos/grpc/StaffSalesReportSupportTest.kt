package com.openpos.pos.grpc

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import openpos.common.v1.DateRange
import openpos.pos.v1.GetStaffSalesReportRequest
import java.time.Instant
import java.util.UUID

class StaffSalesReportSupportTest {
    private val storeId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val staffId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `validates and parses report query`() {
        val query =
            resolveStaffSalesReportQuery(
                GetStaffSalesReportRequest
                    .newBuilder()
                    .setStoreId(storeId.toString())
                    .setDateRange(
                        DateRange
                            .newBuilder()
                            .setStart("2026-05-01")
                            .setEnd("2026-05-07")
                            .build(),
                    ).build(),
            )

        assertEquals(storeId, query.storeId)
        assertEquals(Instant.parse("2026-05-01T00:00:00Z"), query.startDate)
        assertEquals(Instant.parse("2026-05-08T00:00:00Z"), query.endDate)
    }

    @Test
    fun `rejects missing required fields`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                resolveStaffSalesReportQuery(GetStaffSalesReportRequest.getDefaultInstance())
            }

        assertEquals("store_id is required", error.message)
    }

    @Test
    fun `builds staff sales items with average and id fallback`() {
        val items =
            buildStaffSalesItems(
                aggregatedRows =
                    listOf(
                        arrayOf(staffId, 3L, 9000L),
                        arrayOf(UUID.fromString("33333333-3333-3333-3333-333333333333"), 0L, 0L),
                    ),
                staffNameMap = mapOf(staffId to "Aiko"),
            )

        assertEquals(2, items.size)
        assertEquals("Aiko", items[0].staffName)
        assertEquals(3000, items[0].averageTransaction)
        assertEquals("33333333-3333-3333-3333-333333333333", items[1].staffName)
        assertEquals(0, items[1].averageTransaction)
    }

    @Test
    fun `falls back to empty map when staff lookup fails`() {
        val names =
            loadStaffNameMapOrEmpty(
                organizationId = UUID.fromString("44444444-4444-4444-4444-444444444444"),
                storeId = storeId,
            ) { _, _ ->
                throw IllegalStateException("grpc unavailable")
            }

        assertEquals(emptyMap<UUID, String>(), names)
    }
}
