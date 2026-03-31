package com.openpos.analytics.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMessage
import io.smallrye.common.annotation.Blocking
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.jboss.logging.Logger
import java.nio.charset.StandardCharsets
import java.util.UUID
import java.util.concurrent.CompletionStage
import io.vertx.core.json.JsonObject

/**
 * RabbitMQ からイベントを受信し、冪等ハンドラー経由で処理する。
 * sale.completed -> 売上集計更新、sale.voided -> 売上集計ロールバック
 * 明示的に ack/nack を行い、処理完了前の ack を防止する。
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

    @Blocking
    @Incoming("sale-completed-events")
    fun onSaleCompleted(message: IncomingRabbitMQMessage<*>): CompletionStage<Void> =
        try {
            val body = payloadAsString(message.payload)
            val envelope = objectMapper.readValue(body, EventEnvelopeDto::class.java)
            val eventId = UUID.fromString(envelope.eventId)

            idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
                val payload = objectMapper.readValue(envelope.payload, SaleCompletedPayload::class.java)
                salesEventProcessor.processSaleCompleted(
                    UUID.fromString(envelope.organizationId),
                    payload,
                )
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process sale-completed event, sending nack")
            message.nack(e)
        }

    @Blocking
    @Incoming("sale-voided-events")
    fun onSaleVoided(message: IncomingRabbitMQMessage<*>): CompletionStage<Void> =
        try {
            val body = payloadAsString(message.payload)
            val envelope = objectMapper.readValue(body, EventEnvelopeDto::class.java)
            val eventId = UUID.fromString(envelope.eventId)

            idempotentHandler.handleIdempotent(eventId, envelope.eventType) {
                val payload = objectMapper.readValue(envelope.payload, SaleVoidedPayload::class.java)
                salesEventProcessor.processSaleVoided(
                    UUID.fromString(envelope.organizationId),
                    payload,
                )
            }
            message.ack()
        } catch (e: Exception) {
            log.errorf(e, "Failed to process sale-voided event, sending nack")
            message.nack(e)
        }

    private fun payloadAsString(payload: Any?): String =
        when (payload) {
            null -> throw IllegalArgumentException("RabbitMQ payload is null")
            is String -> payload
            is ByteArray -> String(payload, StandardCharsets.UTF_8)
            is JsonObject -> payload.encode()
            else -> objectMapper.writeValueAsString(payload)
        }
}
