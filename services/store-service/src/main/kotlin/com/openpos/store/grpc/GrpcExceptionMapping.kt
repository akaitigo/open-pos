package com.openpos.store.grpc

import io.grpc.Status
import io.grpc.StatusRuntimeException

open class InvalidInputException(message: String) : RuntimeException(message)
open class BusinessPreconditionException(message: String) : RuntimeException(message)
open class ResourceNotFoundException(message: String) : RuntimeException(message)

fun mapToGrpcException(e: Exception): StatusRuntimeException =
    when (e) {
        is InvalidInputException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        is ResourceNotFoundException -> Status.NOT_FOUND.withDescription(e.message).asRuntimeException()
        is BusinessPreconditionException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        is IllegalArgumentException -> Status.INVALID_ARGUMENT.withDescription(e.message).asRuntimeException()
        is IllegalStateException -> Status.FAILED_PRECONDITION.withDescription(e.message).asRuntimeException()
        is StatusRuntimeException -> e
        else -> {
            val log = org.jboss.logging.Logger.getLogger("GrpcExceptionMapping")
            log.errorf(e, "Unhandled exception in gRPC handler")
            Status.INTERNAL.withDescription("Internal server error").asRuntimeException()
        }
    }
