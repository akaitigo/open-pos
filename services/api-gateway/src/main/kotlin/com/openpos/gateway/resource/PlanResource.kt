package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * プラン・サブスクリプション管理 REST リソース。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * RBAC: OWNER のみアクセス可能。
 */
@Path("/api/plans")
@Blocking
@Timeout(30000)
class PlanResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Plan API is not yet implemented"))
            .build()

    @GET
    fun listPlans(): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }

    @GET
    @Path("/current")
    fun getCurrentPlan(): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }

    @PUT
    @Path("/change")
    fun changePlan(body: ChangePlanBody): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }

    @POST
    @Path("/subscribe")
    fun subscribe(body: SubscribeBody): Response {
        tenantContext.requireRole("OWNER")
        return notImplemented()
    }
}

data class ChangePlanBody(
    val planId: String,
)

data class SubscribeBody(
    val planId: String,
)
