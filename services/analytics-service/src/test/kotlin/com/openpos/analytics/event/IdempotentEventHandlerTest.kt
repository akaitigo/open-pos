package com.openpos.analytics.event

import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class IdempotentEventHandlerTest {
    @Inject
    lateinit var idempotentHandler: IdempotentEventHandler

    @InjectMock
    lateinit var processedEventRepository: ProcessedEventRepository

    @Test
    fun `first processing executes action and returns true`() {
        // Arrange
        val eventId = UUID.randomUUID()
        var actionExecuted = false
        whenever(processedEventRepository.isProcessed(eventId)).thenReturn(false)
        doNothing().whenever(processedEventRepository).persist(any<ProcessedEventEntity>())

        // Act
        val result =
            idempotentHandler.handleIdempotent(eventId, "sale.completed") {
                actionExecuted = true
            }

        // Assert
        assertTrue(result)
        assertTrue(actionExecuted)
        verify(processedEventRepository).persist(any<ProcessedEventEntity>())
    }

    @Test
    fun `duplicate event skips action and returns false`() {
        // Arrange
        val eventId = UUID.randomUUID()
        var actionExecuted = false
        whenever(processedEventRepository.isProcessed(eventId)).thenReturn(true)

        // Act
        val result =
            idempotentHandler.handleIdempotent(eventId, "sale.completed") {
                actionExecuted = true
            }

        // Assert
        assertFalse(result)
        assertFalse(actionExecuted)
        verify(processedEventRepository, never()).persist(any<ProcessedEventEntity>())
    }
}
