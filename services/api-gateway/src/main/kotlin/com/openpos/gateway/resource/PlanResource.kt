package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * プラン・サブスクリプション管理 REST リソース。
 * プレースホルダー実装。本番では store-service の gRPC を呼び出す。
 */
@Path("/api/plans")
@Blocking
class PlanResource {
    @GET
    fun listPlans(): List<Map<String, Any>> {
        // プレースホルダー: 固定プラン一覧を返す
        return listOf(
            mapOf(
                "id" to "free-plan",
                "name" to "フリー",
                "maxStores" to 1,
                "maxTerminals" to 2,
                "maxProducts" to 100,
                "monthlyPrice" to 0,
                "isActive" to true,
            ),
            mapOf(
                "id" to "starter-plan",
                "name" to "スターター",
                "maxStores" to 3,
                "maxTerminals" to 10,
                "maxProducts" to 1000,
                "monthlyPrice" to 298000,
                "isActive" to true,
            ),
            mapOf(
                "id" to "business-plan",
                "name" to "ビジネス",
                "maxStores" to 10,
                "maxTerminals" to 50,
                "maxProducts" to 10000,
                "monthlyPrice" to 980000,
                "isActive" to true,
            ),
        )
    }

    @POST
    @Path("/subscribe")
    fun subscribe(body: SubscribeBody): Response {
        // プレースホルダー
        return Response
            .status(Response.Status.CREATED)
            .entity(
                mapOf(
                    "planId" to body.planId,
                    "status" to "ACTIVE",
                    "message" to "サブスクリプションを開始しました",
                ),
            ).build()
    }
}

data class SubscribeBody(
    val planId: String,
)
