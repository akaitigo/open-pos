package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response

/**
 * プラン・サブスクリプション管理 REST リソース。
 * 未実装: gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 */
@Path("/api/plans")
@Blocking
class PlanResource {
    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Plan API is not yet implemented"))
            .build()

    @GET
    fun listPlans(): Response = notImplemented()

    @POST
    @Path("/subscribe")
    fun subscribe(body: SubscribeBody): Response = notImplemented()
}

data class SubscribeBody(
    val planId: String,
)
