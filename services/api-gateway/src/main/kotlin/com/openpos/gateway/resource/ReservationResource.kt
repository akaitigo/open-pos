package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
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
import openpos.pos.v1.CancelReservationRequest
import openpos.pos.v1.CreateReservationRequest
import openpos.pos.v1.FulfillReservationRequest
import openpos.pos.v1.GetReservationRequest
import openpos.pos.v1.ListReservationsRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

/**
 * 予約注文 REST リソース（#193）。
 * RBAC: 予約一覧は全ロール、作成/履行/キャンセルは OWNER/MANAGER のみ。
 */
@Path("/api/reservations")
@Blocking
@Timeout(30000)
class ReservationResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(
        @QueryParam("storeId") storeId: String?,
        @QueryParam("status") status: String?,
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER", "CASHIER")
        val request =
            ListReservationsRequest
                .newBuilder()
                .apply {
                    storeId?.let { setStoreId(it) }
                    status?.let { setStatus(it) }
                }.build()
        val response = grpc.withTenant(stub).listReservations(request)
        return mapOf("data" to response.reservationsList.map { it.toMap() })
    }

    @POST
    fun create(body: CreateReservationBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateReservationRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setCustomerName(body.customerName.orEmpty())
                .setCustomerPhone(body.customerPhone.orEmpty())
                .setItems(
                    body.items
                        .joinToString(",", "[", "]") { """{"productId":"${it.productId}","quantity":${it.quantity}}""" },
                ).setReservedUntil(body.reservedUntil)
                .setNote(body.note.orEmpty())
                .build()
        val response = grpc.withTenant(stub).createReservation(request)
        return Response.status(Response.Status.CREATED).entity(response.reservation.toMap()).build()
    }

    @PUT
    @Path("/{id}/fulfill")
    fun fulfill(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request = FulfillReservationRequest.newBuilder().setId(id).build()
        return grpc
            .withTenant(stub)
            .fulfillReservation(request)
            .reservation
            .toMap()
    }

    @PUT
    @Path("/{id}/cancel")
    fun cancel(
        @PathParam("id") id: String,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request = CancelReservationRequest.newBuilder().setId(id).build()
        return grpc
            .withTenant(stub)
            .cancelReservation(request)
            .reservation
            .toMap()
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
