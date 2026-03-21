package com.openpos.pos.grpc

import io.grpc.Status
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GrpcExceptionMappingTest {
    @Test
    fun `未知の例外はINTERNALにマッピングされe_messageは漏洩しない`() {
        val result = mapToGrpcException(RuntimeException("sensitive info"))
        assertEquals(Status.INTERNAL.code, result.status.code)
        assertEquals("Internal server error", result.status.description)
    }

    @Test
    fun `StatusRuntimeExceptionはそのまま返される`() {
        val original = Status.ALREADY_EXISTS.withDescription("exists").asRuntimeException()
        assertEquals(Status.ALREADY_EXISTS.code, mapToGrpcException(original).status.code)
    }
}
