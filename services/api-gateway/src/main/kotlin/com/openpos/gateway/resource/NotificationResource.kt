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
 * 通知 REST リソース。
 * 現在はインメモリの空リストを返す。
 * バックエンド gRPC サービス実装後に接続する。
 */
@Path("/api/notifications")
@Blocking
@Timeout(30000)
class NotificationResource {
    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("50") pageSize: Int,
    ): Map<String, Any> {
        tenantContext.requireRole("OWNER", "MANAGER", "CASHIER")
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
