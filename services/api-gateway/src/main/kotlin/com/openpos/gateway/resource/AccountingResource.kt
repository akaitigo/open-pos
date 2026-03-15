package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 会計ソフト連携 REST リソース（#195 freee / MFクラウド）。
 * プレースホルダー実装。
 */
@Path("/api/accounting")
@Blocking
class AccountingResource {
    /**
     * 日次売上データをエクスポートする（会計ソフト連携用）。
     */
    @GET
    @Path("/export/daily")
    fun exportDailySales(
        @QueryParam("date") date: String,
    ): Map<String, Any> =
        mapOf(
            "date" to date,
            "format" to "csv",
            "status" to "placeholder",
            "message" to "会計ソフト連携はプレースホルダー実装です。freee / MFクラウドの API キー設定後に有効化されます。",
            "data" to emptyList<Any>(),
        )

    /**
     * 期間指定で取引データをエクスポートする。
     */
    @GET
    @Path("/export/transactions")
    fun exportTransactions(
        @QueryParam("startDate") startDate: String,
        @QueryParam("endDate") endDate: String,
    ): Map<String, Any> =
        mapOf(
            "startDate" to startDate,
            "endDate" to endDate,
            "format" to "csv",
            "status" to "placeholder",
            "message" to "会計ソフト連携はプレースホルダー実装です。",
            "data" to emptyList<Any>(),
        )
}
