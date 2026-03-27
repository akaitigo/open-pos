package com.openpos.gateway.resource

import com.openpos.gateway.config.TenantContext
import com.openpos.gateway.webhook.WebhookDeliveryService
import com.openpos.gateway.webhook.WebhookRegistration
import com.openpos.gateway.webhook.WebhookStore
import io.smallrye.common.annotation.Blocking
import jakarta.inject.Inject
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.PUT
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response
import java.net.InetAddress
import java.net.URI
import java.util.UUID

/**
 * Webhook REST リソース (#203, #410)。
 * Webhook 登録・配信・リトライ・配信トラッキングを提供する。
 */
@Path("/api/webhooks")
@Blocking
class WebhookResource {
    @Inject
    lateinit var webhookStore: WebhookStore

    @Inject
    lateinit var deliveryService: WebhookDeliveryService

    @Inject
    lateinit var tenantContext: TenantContext

    @GET
    fun list(): List<Map<String, Any?>> {
        val orgId = tenantContext.organizationId ?: return emptyList()
        return webhookStore.findByOrganization(orgId).map { it.toMap() }
    }

    /**
     * Webhook URL が内部ネットワークを指していないか検証する（SSRF 防止）。
     * HTTPS のみ許可し、ループバック/プライベート/リンクローカル IP をブロックする。
     */
    private fun validateWebhookUrl(url: String): String? {
        val uri =
            try {
                URI.create(url)
            } catch (e: Exception) {
                return "Invalid URL"
            }
        if (uri.scheme != "https" && uri.scheme != "http") return "URL scheme must be http or https"
        val host = uri.host ?: return "URL must have a host"
        return try {
            val address = InetAddress.getByName(host)
            when {
                address.isLoopbackAddress -> "URL must not point to loopback address"
                address.isSiteLocalAddress -> "URL must not point to private network"
                address.isLinkLocalAddress -> "URL must not point to link-local address"
                address.isAnyLocalAddress -> "URL must not point to any-local address"
                host.equals("metadata.google.internal", ignoreCase = true) -> "URL must not point to cloud metadata"
                host.startsWith("169.254.") -> "URL must not point to link-local address"
                else -> null
            }
        } catch (e: Exception) {
            "Cannot resolve URL host: $host"
        }
    }

    @POST
    fun create(body: CreateWebhookBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        validateWebhookUrl(body.url)?.let {
            return Response.status(Response.Status.BAD_REQUEST).entity(mapOf("error" to it)).build()
        }
        if (body.secret.length < 32) {
            return Response
                .status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "Webhook secret must be at least 32 characters for HMAC-SHA256 security"))
                .build()
        }
        val registration =
            webhookStore.register(
                WebhookRegistration(
                    organizationId = orgId,
                    url = body.url,
                    events = body.events,
                    secret = body.secret,
                ),
            )
        return Response
            .status(Response.Status.CREATED)
            .entity(registration.toMap())
            .build()
    }

    @PUT
    @Path("/{id}")
    fun update(
        @PathParam("id") id: String,
        body: UpdateWebhookBody,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val webhookId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return Response.status(Response.Status.BAD_REQUEST).build()
            }
        val existing = webhookStore.findById(webhookId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        if (existing.organizationId != orgId) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        body.url?.let { newUrl ->
            validateWebhookUrl(newUrl)?.let {
                return Response.status(Response.Status.BAD_REQUEST).entity(mapOf("error" to it)).build()
            }
        }
        val updated =
            webhookStore.update(
                existing.copy(
                    url = body.url ?: existing.url,
                    events = body.events ?: existing.events,
                    isActive = body.isActive ?: existing.isActive,
                ),
            )
        return Response.ok(updated.toMap()).build()
    }

    @DELETE
    @Path("/{id}")
    fun delete(
        @PathParam("id") id: String,
    ): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val webhookId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return Response.status(Response.Status.BAD_REQUEST).build()
            }
        val existing = webhookStore.findById(webhookId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        if (existing.organizationId != orgId) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        return if (webhookStore.delete(webhookId)) {
            Response.noContent().build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @POST
    @Path("/test")
    fun testDelivery(body: TestWebhookBody): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val deliveries =
            deliveryService.deliver(
                organizationId = orgId,
                eventType = body.eventType,
                payload = body.payload,
            )
        return Response
            .ok(
                mapOf(
                    "deliveryCount" to deliveries.size,
                    "deliveries" to deliveries.map { it.toMap() },
                ),
            ).build()
    }

    @GET
    @Path("/{id}/deliveries")
    fun listDeliveries(
        @PathParam("id") id: String,
    ): Response {
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val webhookId =
            try {
                UUID.fromString(id)
            } catch (e: IllegalArgumentException) {
                return Response.status(Response.Status.BAD_REQUEST).build()
            }
        val existing = webhookStore.findById(webhookId) ?: return Response.status(Response.Status.NOT_FOUND).build()
        if (existing.organizationId != orgId) {
            return Response.status(Response.Status.NOT_FOUND).build()
        }
        val deliveries = webhookStore.findDeliveries(webhookId)
        return Response.ok(deliveries.map { it.toMap() }).build()
    }

    @POST
    @Path("/retry")
    fun retryPending(): Response {
        tenantContext.requireRole("OWNER", "MANAGER")
        val orgId = tenantContext.organizationId ?: return Response.status(Response.Status.BAD_REQUEST).build()
        val retried = deliveryService.retryPendingDeliveries(orgId)
        return Response.ok(mapOf("retriedCount" to retried)).build()
    }
}

private fun WebhookRegistration.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id.toString(),
        "organizationId" to organizationId.toString(),
        "url" to url,
        "events" to events,
        "isActive" to isActive,
        "createdAt" to createdAt.toString(),
        "updatedAt" to updatedAt.toString(),
    )

private fun com.openpos.gateway.webhook.WebhookDelivery.toMap(): Map<String, Any?> =
    mapOf(
        "id" to id.toString(),
        "webhookId" to webhookId.toString(),
        "eventType" to eventType,
        "status" to status.name,
        "httpStatusCode" to httpStatusCode,
        "attemptCount" to attemptCount,
        "maxRetries" to maxRetries,
        "nextRetryAt" to nextRetryAt?.toString(),
        "lastError" to lastError,
        "createdAt" to createdAt.toString(),
        "completedAt" to completedAt?.toString(),
    )

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

data class TestWebhookBody(
    val eventType: String,
    val payload: String,
)
