package com.openpos.gateway.config

import io.grpc.Status
import io.grpc.StatusRuntimeException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GrpcExceptionMapperTest {
    private val mapper = GrpcExceptionMapper()

    @Nested
    inner class StatusCodeMapping {
        @Test
        fun `INVALID_ARGUMENTは400を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.INVALID_ARGUMENT.withDescription("bad input"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(400, response.status)
        }

        @Test
        fun `NOT_FOUNDは404を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.NOT_FOUND.withDescription("resource not found"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(404, response.status)
        }

        @Test
        fun `ALREADY_EXISTSは409を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.ALREADY_EXISTS.withDescription("duplicate entry"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(409, response.status)
        }

        @Test
        fun `PERMISSION_DENIEDは403を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.PERMISSION_DENIED.withDescription("access denied"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(403, response.status)
        }

        @Test
        fun `UNAUTHENTICATEDは401を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.UNAUTHENTICATED.withDescription("not authenticated"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(401, response.status)
        }

        @Test
        fun `UNAVAILABLEは503を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.UNAVAILABLE.withDescription("service unavailable"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(503, response.status)
        }

        @Test
        fun `DEADLINE_EXCEEDEDは504を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.DEADLINE_EXCEEDED.withDescription("timeout"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(504, response.status)
        }

        @Test
        fun `INTERNALは500を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.INTERNAL.withDescription("internal error"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(500, response.status)
        }

        @Test
        fun `UNKNOWNはelseブランチで500を返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.UNKNOWN.withDescription("unknown error"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            assertEquals(500, response.status)
        }
    }

    @Nested
    inner class ErrorMessage {
        @Test
        fun `descriptionがある場合はメッセージに含まれる`() {
            // Arrange
            val ex = StatusRuntimeException(Status.NOT_FOUND.withDescription("商品が見つかりません"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, String>
            assertEquals("商品が見つかりません", entity["message"])
        }

        @Test
        fun `descriptionがない場合はステータスコード名が使用される`() {
            // Arrange
            val ex = StatusRuntimeException(Status.NOT_FOUND)

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, String>
            assertEquals("NOT_FOUND", entity["message"])
        }

        @Test
        fun `INTERNAL_SERVER_ERRORの場合は固定メッセージを返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.INTERNAL.withDescription("sensitive db error details"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, String>
            assertEquals("Internal server error", entity["message"])
        }

        @Test
        fun `UNKNOWNステータスもINTERNAL_SERVER_ERRORとして固定メッセージを返す`() {
            // Arrange
            val ex = StatusRuntimeException(Status.UNKNOWN.withDescription("unknown internal details"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, String>
            assertEquals("Internal server error", entity["message"])
        }

        @Test
        fun `errorフィールドにHTTPステータスのreasonPhraseが含まれる`() {
            // Arrange
            val ex = StatusRuntimeException(Status.NOT_FOUND.withDescription("not found"))

            // Act
            val response = mapper.toResponse(ex)

            // Assert
            @Suppress("UNCHECKED_CAST")
            val entity = response.entity as Map<String, String>
            assertEquals("Not Found", entity["error"])
        }
    }
}
