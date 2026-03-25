package com.openpos.gateway.config

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider
import java.util.UUID

@Provider
@Priority(Priorities.AUTHENTICATION - 10)
class TenantRequestFilter : ContainerRequestFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    override fun filter(requestContext: ContainerRequestContext) {
        // POST /api/organizations は X-Organization-Id 不要（テナント未作成時に呼ばれるため）
        // ヘッダーがなければ tenantContext.organizationId は null のまま
        val orgIdHeader = requestContext.getHeaderString("X-Organization-Id")
        if (orgIdHeader != null) {
            try {
                tenantContext.organizationId = UUID.fromString(orgIdHeader)
            } catch (_: IllegalArgumentException) {
                requestContext.abortWith(
                    Response
                        .status(Response.Status.BAD_REQUEST)
                        .entity(mapOf("error" to "Invalid X-Organization-Id header: not a valid UUID"))
                        .build(),
                )
            }
        }
    }
}
