package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.Response

/**
 * 通知 REST リソース (#174)。
 * Phase 9: 通知管理の REST API placeholder。
 */
@Path("/api/notifications")
@Blocking
class NotificationResource {
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

    @PUT
    @Path("/{id}/read")
    fun markRead(@PathParam("id") id: String): Response =
        Response.ok(mapOf("id" to id, "isRead" to true)).build()

    @PUT
    @Path("/read-all")
    fun markAllRead(): Response =
        Response.ok(mapOf("action" to "mark_all_read")).build()
}
