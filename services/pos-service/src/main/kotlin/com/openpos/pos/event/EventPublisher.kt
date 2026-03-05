package com.openpos.pos.event

import com.fasterxml.jackson.databind.ObjectMapper
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import java.time.Instant
import java.util.UUID

/**
 * RabbitMQ へイベントを発行する共通コンポーネント。
 * EventEnvelope でラップし、routing key でトピックルーティングする。
 */
@ApplicationScoped
class EventPublisher {
    @Inject
    @Channel("outgoing-events")
    lateinit var emitter: Emitter<String>

    @Inject
    lateinit var objectMapper: ObjectMapper

    /**
     * イベントを発行する。
     * @param eventType routing key (例: "sale.completed")
     * @param organizationId テナントID
     * @param payload イベントペイロード（JSON にシリアライズされる）
     * @return 生成された event_id
     */
    fun publish(
        eventType: String,
        organizationId: UUID,
        payload: Any,
    ): UUID {
        val eventId = UUID.randomUUID()
        val envelope =
            EventEnvelopeDto(
                eventId = eventId.toString(),
                eventType = eventType,
                timestamp = Instant.now().toString(),
                organizationId = organizationId.toString(),
                payload = objectMapper.writeValueAsString(payload),
                source = "pos-service",
            )
        val json = objectMapper.writeValueAsString(envelope)

        val metadata =
            OutgoingRabbitMQMetadata
                .builder()
                .withRoutingKey(eventType)
                .withContentType("application/json")
                .build()

        emitter.send(Message.of(json, Metadata.of(metadata)))
        return eventId
    }
}
