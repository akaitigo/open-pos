package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.pos.v1.CreateSettlementRequest
import openpos.pos.v1.GetSettlementRequest
import openpos.pos.v1.PosServiceGrpc
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/settlements")
@Blocking
@Timeout(30000)
class SettlementResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    fun create(body: CreateSettlementBody): Response {
        val request =
            CreateSettlementRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setTerminalId(body.terminalId)
                .setStaffId(body.staffId)
                .setCashActual(body.cashActual)
                .build()
        val response = grpc.withTenant(stub).createSettlement(request)
        return Response.status(Response.Status.CREATED).entity(response.settlement.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getSettlement(GetSettlementRequest.newBuilder().setId(id).build())
            .settlement
            .toMap()
}

data class CreateSettlementBody(
    val storeId: String,
    val terminalId: String,
    val staffId: String,
    val cashActual: Long,
)
