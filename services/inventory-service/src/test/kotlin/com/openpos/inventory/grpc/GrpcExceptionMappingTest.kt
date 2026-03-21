package com.openpos.inventory.grpc

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
}
