package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * タイムセール REST リソース (#143)。
 * Phase 9: タイムセール管理の REST API placeholder。
 */
@Path("/api/time-sales")
@Blocking
class TimeSaleResource {
    @GET
    fun list(
        @QueryParam("page") @DefaultValue("1") page: Int,
        @QueryParam("pageSize") @DefaultValue("20") pageSize: Int,
        @QueryParam("activeOnly") @DefaultValue("false") activeOnly: Boolean,
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
}
