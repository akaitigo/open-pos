package com.openpos.gateway

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType

@Path("/api/health")
class HealthResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    fun health(): Map<String, String> {
        return mapOf(
            "service" to "api-gateway",
            "status" to "UP"
        )
    }
}
