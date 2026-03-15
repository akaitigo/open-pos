package com.openpos.gateway.config

import io.grpc.Status
import io.grpc.StatusRuntimeException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider

@Provider
class GrpcExceptionMapper : ExceptionMapper<StatusRuntimeException> {
    override fun toResponse(exception: StatusRuntimeException): Response {
        val httpStatus =
            when (exception.status.code) {
                Status.Code.NOT_FOUND -> Response.Status.NOT_FOUND
                Status.Code.INVALID_ARGUMENT -> Response.Status.BAD_REQUEST
                Status.Code.ALREADY_EXISTS -> Response.Status.CONFLICT
                Status.Code.UNAUTHENTICATED -> Response.Status.UNAUTHORIZED
                Status.Code.PERMISSION_DENIED -> Response.Status.FORBIDDEN
                Status.Code.ABORTED -> Response.Status.CONFLICT
                Status.Code.FAILED_PRECONDITION -> Response.Status.PRECONDITION_FAILED
                Status.Code.UNAVAILABLE -> Response.Status.SERVICE_UNAVAILABLE
                Status.Code.DEADLINE_EXCEEDED -> Response.Status.GATEWAY_TIMEOUT
                else -> Response.Status.INTERNAL_SERVER_ERROR
            }
        val message = exception.status.description ?: exception.status.code.name
        return Response
            .status(httpStatus)
            .entity(mapOf("error" to httpStatus.reasonPhrase, "message" to message))
            .build()
    }
}
