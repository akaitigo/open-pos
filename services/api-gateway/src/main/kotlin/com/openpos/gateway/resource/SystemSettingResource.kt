package com.openpos.gateway.resource

import com.openpos.gateway.config.GrpcClientHelper
import com.openpos.gateway.config.toMap
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import openpos.store.v1.DeleteSystemSettingRequest
import openpos.store.v1.GetSystemSettingRequest
import openpos.store.v1.ListSystemSettingsRequest
import openpos.store.v1.SystemSettingServiceGrpc
import openpos.store.v1.UpsertSystemSettingRequest

@Path("/api/settings")
@Blocking
class SystemSettingResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: SystemSettingServiceGrpc.SystemSettingServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @GET
    fun list(): List<Map<String, Any?>> {
        val response = grpc.withTenant(stub).listSystemSettings(ListSystemSettingsRequest.getDefaultInstance())
        return response.settingsList.map { it.toMap() }
    }

    @GET
    @Path("/{key}")
    fun get(
        @PathParam("key") key: String,
    ): Map<String, Any?> =
        grpc
            .withTenant(stub)
            .getSystemSetting(GetSystemSettingRequest.newBuilder().setKey(key).build())
            .setting
            .toMap()

    @PUT
    @Path("/{key}")
    fun upsert(
        @PathParam("key") key: String,
        body: UpsertSettingBody,
    ): Map<String, Any?> {
        val request =
            UpsertSystemSettingRequest
                .newBuilder()
                .setKey(key)
                .setValue(body.value)
                .apply { body.description?.let { setDescription(it) } }
                .build()
        return grpc
            .withTenant(stub)
            .upsertSystemSetting(request)
            .setting
            .toMap()
    }

    @DELETE
    @Path("/{key}")
    fun delete(
        @PathParam("key") key: String,
    ): Response {
        grpc.withTenant(stub).deleteSystemSetting(DeleteSystemSettingRequest.newBuilder().setKey(key).build())
        return Response.noContent().build()
    }
}

data class UpsertSettingBody(
    val value: String,
    val description: String? = null,
)
