package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam

/**
 * 売上目標管理 REST リソース（#214）。
 * プレースホルダー実装。
 */
@Path("/api/sales-targets")
@Blocking
class SalesTargetResource {
    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("month") month: String?,
    ): List<Map<String, Any>> {
        // プレースホルダー
        return emptyList()
    }

    @PUT
    fun upsert(body: UpsertSalesTargetBody): Map<String, Any> =
        mapOf(
            "storeId" to (body.storeId ?: ""),
            "targetMonth" to body.targetMonth,
            "targetAmount" to body.targetAmount,
            "message" to "売上目標を設定しました",
        )
}

data class UpsertSalesTargetBody(
    val storeId: String? = null,
    val targetMonth: String,
    val targetAmount: Long,
)
