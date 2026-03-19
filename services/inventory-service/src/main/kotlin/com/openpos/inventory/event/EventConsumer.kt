package com.openpos.inventory.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.util.UUID
import java.util.concurrent.CompletionStage

/**
 * RabbitMQ からイベントを受信し、冪等ハンドラー経由で処理する。
 * sale.completed -> 在庫減算、sale.voided -> 在庫戻し
 * 明示的に ack/nack を行い、処理完了前の ack を防止する。
 */
@ApplicationScoped
class EventConsumer {
    @Inject
    lateinit var idempotentHandler: IdempotentEventHandler

    @Inject
    lateinit var stockEventProcessor: StockEventProcessor

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val log = Logger.getLogger(EventConsumer::class.java)

    @Incoming("sale-completed")
    fun onSaleCompleted(message: IncomingRabbitMQMessage<String>): CompletionStage<Void> =
        try {
            val body = message.payload
            val envelope = objectMapper.readValue(body, EventEnvelopeDto::class.java)
            val eventId = UUID.fromString(envelope.eventId)

            idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
                val payload = objectMapper.readValue(envelope.payload, SaleCompletedPayload::class.java)
                stockEventProcessor.processSaleCompleted(
                    UUID.fromString(envelope.organizationId),
                    payload,
                )
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process sale-completed event, sending nack")
            message.nack(e)
        }

    @Incoming("sale-voided")
    fun onSaleVoided(message: IncomingRabbitMQMessage<String>): CompletionStage<Void> =
        try {
            val body = message.payload
            val envelope = objectMapper.readValue(body, EventEnvelopeDto::class.java)
            val eventId = UUID.fromString(envelope.eventId)

            idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
                val payload = objectMapper.readValue(envelope.payload, SaleVoidedPayload::class.java)
                stockEventProcessor.processSaleVoided(
                    UUID.fromString(envelope.organizationId),
                    payload,
                )
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process sale-voided event, sending nack")
            message.nack(e)
        }
}
