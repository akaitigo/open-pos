package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 監査ログ REST リソース（#187 スタッフ活動ログ）。
 * プレースホルダー実装。スタッフ、操作種別、日付範囲でフィルタ可能。
 */
@Path("/api/audit-logs")
@Blocking
class AuditLogResource {
    @GET
    fun list(
        @QueryParam("staffId") staffId: String?,
        @QueryParam("action") action: String?,
        @QueryParam("startDate") startDate: String?,
        @QueryParam("endDate") endDate: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        // プレースホルダー: 空リストを返す
        return mapOf(
            "data" to emptyList<Any>(),
            "pagination" to
                mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "totalCount" to 0,
                    "totalPages" to 0,
                ),
            "filters" to
                mapOf(
                    "staffId" to (staffId ?: ""),
                    "action" to (action ?: ""),
                    "startDate" to (startDate ?: ""),
                    "endDate" to (endDate ?: ""),
                ),
        )
    }
}
