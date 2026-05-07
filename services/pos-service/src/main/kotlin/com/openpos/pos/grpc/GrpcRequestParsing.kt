package com.openpos.pos.grpc

import io.grpc.Status
import java.util.UUID

internal fun String.toUUID(): UUID =
    try {
        UUID.fromString(this)
    } catch (e: IllegalArgumentException) {
        throw Status.INVALID_ARGUMENT.withDescription("Invalid UUID: $this").asRuntimeException()
    }

internal fun String.uuidOrNull(): UUID? = if (isBlank()) null else toUUID()
