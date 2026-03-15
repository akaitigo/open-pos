package com.openpos.gateway.resource

import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
import io.quarkus.grpc.GrpcClient
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response
import openpos.store.v1.StoreServiceGrpc

/**
 * 顧客管理 REST リソース (#140)。
 * gRPC store-service 経由で顧客 CRUD を提供する。
 * Phase 9: 顧客管理の REST API placeholder。
 */
@Path("/api/customers")
@Blocking
class CustomerResource {
    @Inject
    @GrpcClient("store-service")
    lateinit var stub: StoreServiceGrpc.StoreServiceBlockingStub

    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var cache: RedisCacheService

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("search") search: String?,
    ): Map<String, Any> {
        // Placeholder - Phase 9 gRPC methods not yet in proto
        return mapOf(
            "data" to emptyList<Any>(),
            "pagination" to mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "totalCount" to 0,
                "totalPages" to 0,
            ),
        )
    }

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: String): Response =
        Response.status(Response.Status.NOT_FOUND).build()

    @POST
    fun create(body: Map<String, Any?>): Response =
        Response.status(Response.Status.CREATED).entity(body).build()

    @PUT
    @Path("/{id}")
    fun update(@PathParam("id") id: String, body: Map<String, Any?>): Response =
        Response.ok(body).build()

    @DELETE
    @Path("/{id}")
    fun delete(@PathParam("id") id: String): Response =
        Response.noContent().build()

    @POST
    @Path("/{id}/points/earn")
    fun earnPoints(@PathParam("id") id: String, body: Map<String, Any?>): Response =
        Response.ok(mapOf("customerId" to id, "action" to "earn")).build()

    @POST
    @Path("/{id}/points/redeem")
    fun redeemPoints(@PathParam("id") id: String, body: Map<String, Any?>): Response =
        Response.ok(mapOf("customerId" to id, "action" to "redeem")).build()
}
