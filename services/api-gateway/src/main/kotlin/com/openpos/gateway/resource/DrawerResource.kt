package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.pos.v1.CloseDrawerRequest
import openpos.pos.v1.GetDrawerStatusRequest
import openpos.pos.v1.OpenDrawerRequest
import openpos.pos.v1.PosServiceGrpc

@Path("/api/drawers")
@Blocking
class DrawerResource {
    @Inject
    @GrpcClient("pos-service")
    lateinit var stub: PosServiceGrpc.PosServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @POST
    @Path("/open")
    fun open(body: OpenDrawerBody): Response {
        val request =
            OpenDrawerRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setTerminalId(body.terminalId)
                .setOpeningAmount(body.openingAmount)
                .build()
        val response = grpc.withTenant(stub).openDrawer(request)
        return Response.status(Response.Status.CREATED).entity(response.drawer.toMap()).build()
    }

    @POST
    @Path("/close")
    fun close(body: CloseDrawerBody): Map<String, Any?> {
        val request =
            CloseDrawerRequest
                .newBuilder()
                .setStoreId(body.storeId)
                .setTerminalId(body.terminalId)
                .build()
        return grpc
            .withTenant(stub)
            .closeDrawer(request)
            .drawer
            .toMap()
    }

    @GET
    @Path("/status")
    fun getStatus(
        @QueryParam("storeId") storeId: String,
        @QueryParam("terminalId") terminalId: String,
    ): Map<String, Any?> {
        val request =
            GetDrawerStatusRequest
                .newBuilder()
                .setStoreId(storeId)
                .setTerminalId(terminalId)
                .build()
        return grpc
            .withTenant(stub)
            .getDrawerStatus(request)
            .drawer
            .toMap()
    }
}

data class OpenDrawerBody(
    val storeId: String,
    val terminalId: String,
    val openingAmount: Long,
)

data class CloseDrawerBody(
    val storeId: String,
    val terminalId: String,
)
