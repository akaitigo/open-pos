package com.openpos.gateway.resource

import com.openpos.gateway.cache.RedisCacheService
import com.openpos.gateway.config.GrpcClientHelper
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

/**
 * ギフトカード REST リソース (#142)。
 * Phase 9: ギフトカード管理の REST API placeholder。
 */
@Path("/api/gift-cards")
@Blocking
class GiftCardResource {
    @Inject
    lateinit var grpc: GrpcClientHelper

    @Inject
    lateinit var cache: RedisCacheService

    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
    ): Map<String, Any> =
        mapOf(
            "data" to emptyList<Any>(),
            "pagination" to mapOf(
                "page" to page,
                "pageSize" to pageSize,
                "totalCount" to 0,
                "totalPages" to 0,
            ),
        )

    @GET
    @Path("/{id}")
    fun get(@PathParam("id") id: String): Response =
        Response.status(Response.Status.NOT_FOUND).build()

    @POST
    fun create(body: Map<String, Any?>): Response =
        Response.status(Response.Status.CREATED).entity(body).build()

    @POST
    @Path("/{code}/activate")
    fun activate(@PathParam("code") code: String): Response =
        Response.ok(mapOf("code" to code, "status" to "ACTIVE")).build()

    @POST
    @Path("/{code}/redeem")
    fun redeem(@PathParam("code") code: String, body: Map<String, Any?>): Response =
        Response.ok(mapOf("code" to code, "action" to "redeem")).build()
}
