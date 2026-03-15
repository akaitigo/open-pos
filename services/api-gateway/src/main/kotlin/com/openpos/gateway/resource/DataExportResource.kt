package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

/**
 * テナントデータエクスポート REST リソース（#220）。
 * プレースホルダー実装。本番ではバックグラウンドジョブで大容量データを処理する。
 */
@Path("/api/data")
@Blocking
class DataExportResource {
    /**
     * テナントデータを JSON アーカイブとしてエクスポートする。
     */
    @GET
    @Path("/export")
    fun exportData(): Map<String, Any> =
        mapOf(
            "status" to "placeholder",
            "format" to "json",
            "message" to "テナントデータエクスポートはプレースホルダー実装です。本番ではバックグラウンドジョブで処理します。",
            "sections" to
                listOf(
                    "organizations",
                    "stores",
                    "staff",
                    "products",
                    "categories",
                    "transactions",
                    "inventory",
                ),
        )

    /**
     * テナントデータをインポートする（移行用）。
     */
    @POST
    @Path("/import")
    fun importData(body: Map<String, Any>): Response =
        Response
            .ok(
                mapOf(
                    "status" to "placeholder",
                    "message" to "テナントデータインポートはプレースホルダー実装です。",
                ),
            ).build()
}
