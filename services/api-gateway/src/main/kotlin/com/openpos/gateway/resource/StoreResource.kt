package com.openpos.gateway.resource

import com.google.protobuf.BoolValue
import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.config.paginatedResponse
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
import openpos.common.v1.PaginationRequest
import openpos.store.v1.CreateStoreRequest
import openpos.store.v1.GetStoreRequest
import openpos.store.v1.ListStoresRequest
import openpos.store.v1.ListTerminalsRequest
import openpos.store.v1.RegisterTerminalRequest
import openpos.store.v1.StoreServiceGrpc
import openpos.store.v1.UpdateStoreRequest
import openpos.store.v1.UpdateTerminalSyncRequest
import org.eclipse.microprofile.faulttolerance.Timeout

@Path("/api/stores")
@Blocking
@Timeout(30000)
class StoreResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var tenantContext: TenantContext

    @POST
    fun create(body: CreateStoreBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            CreateStoreRequest
                .newBuilder()
                .setName(body.name)
                .apply {
                    body.address?.let { setAddress(it) }
                    body.phone?.let { setPhone(it) }
                    body.timezone?.let { setTimezone(it) }
                    body.settings?.let { setSettings(it) }
                }.build()
        val response = grpc.withTenant(stub).createStore(request)
        return Response.status(Response.Status.CREATED).entity(response.store.toMap()).build()
    }

    @GET
    @Path("/{id}")
    fun get(
        @PathParam("id") id: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getStore(GetStoreRequest.newBuilder().setId(id).build())
            .store
            .toMap()

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> {
        val request =
            ListStoresRequest
                .newBuilder()
                .setPagination(
                    PaginationRequest
                        .newBuilder()
                        .setPage(page)
                        .setPageSize(pageSize)
                        .build(),
                ).build()
        val response = grpc.withTenant(stub).listStores(request)
        return paginatedResponse(
            data = response.storesList.map { it.toMap() },
            pagination = response.pagination,
        )
    }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateStoreBody,
    ): Map<String, Any?> {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            UpdateStoreRequest
                .newBuilder()
                .setId(id)
                .apply {
                    body.name?.let { setName(it) }
                    body.address?.let { setAddress(it) }
                    body.phone?.let { setPhone(it) }
                    body.timezone?.let { setTimezone(it) }
                    body.settings?.let { setSettings(it) }
                    body.isActive?.let { setIsActiveValue(BoolValue.of(it)) }
                }.build()
        return grpc
            .withTenant(stub)
            .updateStore(request)
            .store
            .toMap()
    }

    // === Terminal sub-resources ===

    @POST
    @Path("/{storeId}/terminals")
    fun registerTerminal(
        @PathParam("storeId") storeId: String,
        body: RegisterTerminalBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val request =
            RegisterTerminalRequest
                .newBuilder()
                .setStoreId(storeId)
                .setTerminalCode(body.terminalCode)
                .setName(body.name)
                .build()
        val response = grpc.withTenant(stub).registerTerminal(request)
        return Response.status(Response.Status.CREATED).entity(response.terminal.toMap()).build()
    }

    @GET
    @Path("/{storeId}/terminals")
    fun listTerminals(
        @PathParam("storeId") storeId: String,
    ): List<Map<String, Any?>> =
        grpc
            .withTenant(stub)
            .listTerminals(ListTerminalsRequest.newBuilder().setStoreId(storeId).build())
            .terminalsList
            .map { it.toMap() }

    @PUT
    @Path("/{storeId}/terminals/{terminalId}/sync")
    fun updateTerminalSync(
        @PathParam("storeId") storeId: String,
        @PathParam("terminalId") terminalId: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .updateTerminalSync(UpdateTerminalSyncRequest.newBuilder().setTerminalId(terminalId).build())
            .terminal
            .toMap()
}

data class CreateStoreBody(
    val name: String,
    val address: String? = null,
    val phone: String? = null,
    val timezone: String? = null,
    val settings: String? = null,
)

data class UpdateStoreBody(
    val name: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val timezone: String? = null,
    val settings: String? = null,
    val isActive: Boolean? = null,
)

data class RegisterTerminalBody(
    val terminalCode: String,
    val name: String,
)
