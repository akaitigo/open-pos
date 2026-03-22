package com.openpos.gateway.config

import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class ForbiddenExceptionMapper : ExceptionMapper<ForbiddenException> {
    override fun toResponse(exception: ForbiddenException): Response =
        Response
            .status(Response.Status.FORBIDDEN)
            .entity(mapOf("error" to "Forbidden", "message" to exception.message))
            .build()
}
