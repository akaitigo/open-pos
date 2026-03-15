package com.openpos.gateway.resource

import io.smallrye.common.annotation.Blocking
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

/**
 * Webhook REST リソース（#203）。
 * プレースホルダー実装。
 */
@Path("/api/webhooks")
@Blocking
class WebhookResource {
    @GET
    fun list(): List<Map<String, Any>> {
        // プレースホルダー
        return emptyList()
    }

    @POST
    fun create(body: CreateWebhookBody): Response =
        Response
            .status(Response.Status.CREATED)
            .entity(
                mapOf(
                    "url" to body.url,
                    "events" to body.events,
                    "isActive" to true,
                    "message" to "Webhookを作成しました",
                ),
            ).build()

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateWebhookBody,
    ): Map<String, Any?> =
        mapOf(
            "id" to id,
            "url" to body.url,
            "events" to body.events,
            "isActive" to body.isActive,
        )

    @DELETE
    @Path("/{id}")
    fun delete(
        @PathParam("id") id: String,
    ): Response = Response.noContent().build()
}

data class CreateWebhookBody(
    val url: String,
    val events: List<String>,
    val secret: String,
)

data class UpdateWebhookBody(
    val url: String? = null,
    val events: List<String>? = null,
    val isActive: Boolean? = null,
)
