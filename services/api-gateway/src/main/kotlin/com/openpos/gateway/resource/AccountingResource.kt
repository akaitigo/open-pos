package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * 会計ソフト連携 REST リソース（#195 freee / MFクラウド）。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * RBAC: OWNER のみアクセス可能。
 */
@Path("/api/accounting")
@Blocking
@Timeout(30000)
class AccountingResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Accounting API is not yet implemented"))
            .build()

    @GET
    @Path("/export/daily")
    fun exportDailySales(
        @QueryParam("date") date: String,
    ): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }

    @GET
    @Path("/export/transactions")
    fun exportTransactions(
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }
}
