package com.openpos.analytics.event

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.util.UUID

/**
 * RabbitMQ からイベントを受信し、冪等ハンドラー経由で処理する。
 * sale.completed -> 売上集計更新、sale.voided -> 売上集計ロールバック
 */
@ApplicationScoped
class EventConsumer {
    @Inject
    lateinit var idempotentHandler: IdempotentEventHandler

    @Inject
    lateinit var salesEventProcessor: SalesEventProcessor

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(EventConsumer::class.java)

    @Incoming("sale-completed-events")
    fun onSaleCompleted(message: String) {
        val envelope = objectMapper.readValue(message, EventEnvelopeDto::class.java)
        val eventId = UUID.fromString(envelope.eventId)

        idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
            val payload = objectMapper.readValue(envelope.payload, SaleCompletedPayload::class.java)
            salesEventProcessor.processSaleCompleted(
                UUID.fromString(envelope.organizationId),
                payload,
            )
        }
    }

    @Incoming("sale-voided-events")
    fun onSaleVoided(message: String) {
        val envelope = objectMapper.readValue(message, EventEnvelopeDto::class.java)
        val eventId = UUID.fromString(envelope.eventId)

        idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
            val payload = objectMapper.readValue(envelope.payload, SaleVoidedPayload::class.java)
            salesEventProcessor.processSaleVoided(
                UUID.fromString(envelope.organizationId),
                payload,
            )
        }
    }
}
