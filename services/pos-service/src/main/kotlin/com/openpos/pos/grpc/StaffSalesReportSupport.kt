package com.openpos.pos.grpc

import openpos.pos.v1.GetStaffSalesReportRequest
import openpos.pos.v1.StaffSalesItem
import java.time.Instant
import java.util.UUID

internal data class StaffSalesReportQuery(
    val storeId: UUID,
    val startDate: Instant,
    val endDate: Instant,
)

internal fun resolveStaffSalesReportQuery(request: GetStaffSalesReportRequest): StaffSalesReportQuery {
    require(request.storeId.isNotBlank()) { "store_id is required" }
    require(request.hasDateRange()) { "date_range is required" }
    require(request.dateRange.start.isNotBlank()) { "date_range.start is required" }
    require(request.dateRange.end.isNotBlank()) { "date_range.end is required" }

    return StaffSalesReportQuery(
        storeId = request.storeId.toUUID(),
        startDate = parseInstantOrDate(request.dateRange.start),
        endDate = parseInstantOrDateExclusive(request.dateRange.end),
    )
}

internal fun loadStaffNameMapOrEmpty(
    organizationId: UUID,
    storeId: UUID,
    loadStaffNameMap: (UUID, UUID) -> Map<UUID, String>,
): Map<UUID, String> =
    try {
        loadStaffNameMap(organizationId, storeId)
    } catch (_: Exception) {
        emptyMap()
    }

internal fun buildStaffSalesItems(
    aggregatedRows: List<Array<Any>>,
    staffNameMap: Map<UUID, String>,
): List<StaffSalesItem> =
    aggregatedRows.map { row ->
        val staffId = row[0] as UUID
        val transactionCount = (row[1] as Long).toInt()
        val totalAmount = row[2] as Long
        val averageTransaction = if (transactionCount > 0) totalAmount / transactionCount else 0L

        StaffSalesItem
            .newBuilder()
            .setStaffId(staffId.toString())
            .setStaffName(staffNameMap[staffId] ?: staffId.toString())
            .setTotalAmount(totalAmount)
            .setTransactionCount(transactionCount)
            .setAverageTransaction(averageTransaction)
            .build()
    }
