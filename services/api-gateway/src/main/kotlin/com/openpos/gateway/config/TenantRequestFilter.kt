package com.openpos.gateway.config

import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import java.util.UUID

@Provider
@Priority(Priorities.AUTHENTICATION + 10)
class TenantRequestFilter : ContainerRequestFilter {
    @Inject
    lateinit var tenantContext: TenantContext

    override fun filter(requestContext: ContainerRequestContext) {
        val orgIdHeader = requestContext.getHeaderString("X-Organization-Id")
        if (orgIdHeader != null) {
            try {
                tenantContext.organizationId = UUID.fromString(orgIdHeader)
            } catch (_: IllegalArgumentException) {
                // Invalid UUID — will be caught downstream if required
            }
        }
    }
}
