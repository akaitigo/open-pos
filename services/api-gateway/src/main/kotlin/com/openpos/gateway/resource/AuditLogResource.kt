package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * 操作履歴（Audit Log）REST リソース（#187）。
 * 現在はインメモリの空リストを返す。
 * バックエンド gRPC サービス実装後に接続する。
 */
@Path("/api/audit-logs")
@Blocking
@Timeout(30000)
class AuditLogResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(
        @QueryParam("staffId") staffId: String?,
        @QueryParam("action") action: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER")
        return mapOf(
            "data" to emptyList<Any>(),
            "pagination" to
                mapOf(
                    "page" to page,
                    "pageSize" to pageSize,
                    "totalCount" to 0,
                    "totalPages" to 0,
                ),
        )
    }
}
