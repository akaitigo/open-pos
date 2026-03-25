package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 会計ソフト連携 REST リソース（#195 freee / MFクラウド）。
 * 未実装: 外部 API 連携が未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/accounting")
@Blocking
class AccountingResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Accounting API is not yet implemented"))
            .build()

    @GET
    @Path("/export/daily")
    fun exportDailySales(
        @QueryParam("date") date: String,
    ): Response = notImplemented()

    @GET
    @Path("/export/transactions")
    fun exportTransactions(
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Response = notImplemented()
}
