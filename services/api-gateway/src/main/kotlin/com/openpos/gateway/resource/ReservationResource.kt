package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * 予約注文 REST リソース（#193）。
 * gRPC バックエンドが未整備のため全エンドポイントが 501 を返す。
 * RBAC: 予約一覧は全ロール、作成/履行/キャンセルは OWNER/MANAGER のみ。
 */
@Path("/api/reservations")
@Blocking
@Timeout(30000)
class ReservationResource {
    @Inject
    lateinit var tenantContext: TenantContext

    private fun notImplemented(): Response =
        Response
            .status(501)
            .entity(mapOf("error" to "NOT_IMPLEMENTED", "message" to "Reservation API is not yet implemented"))
            .build()

    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER", "CASHIER")
        return notImplemented()
    }

    @POST
    fun create(body: CreateReservationBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplemented()
    }

    @PUT
    @Path("/{id}/fulfill")
    fun fulfill(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplemented()
    }

    @PUT
    @Path("/{id}/cancel")
    fun cancel(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        return notImplemented()
    }
}

data class CreateReservationBody(
    val storeId: String,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val items: List<ReservationItemBody> = emptyList(),
    val reservedUntil: String,
    val note: String? = null,
)

data class ReservationItemBody(
    val productId: String,
    val quantity: Int,
)
