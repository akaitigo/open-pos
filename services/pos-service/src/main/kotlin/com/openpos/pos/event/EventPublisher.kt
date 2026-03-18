package com.openpos.pos.event

import com.fasterxml.jackson.databind.ObjectMapper
import com.openpos.pos.entity.OutboxEventEntity
import com.openpos.pos.repository.OutboxRepository
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.eclipse.microprofile.reactive.messaging.Channel
import org.eclipse.microprofile.reactive.messaging.Emitter
import org.eclipse.microprofile.reactive.messaging.Message
import org.eclipse.microprofile.reactive.messaging.Metadata
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * RabbitMQ へイベントを発行する共通コンポーネント。
 * EventEnvelope でラップし、routing key でトピックルーティングする。
 * 送信失敗時はアウトボックステーブルにフォールバック保存し、
 * OutboxProcessor による非同期リトライを保証する。
 */
@ApplicationScoped
class EventPublisher {
    @Inject
    @Channel("outgoing-events")
    lateinit var emitter: Emitter<String>

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var outboxRepository: OutboxRepository

    private val log = Logger.getLogger(EventPublisher::class.java)

    /**
     * イベントを発行する。
     * RabbitMQ への送信が失敗した場合、アウトボックステーブルに保存してリトライに備える。
     * @param eventType routing key (例: "sale.completed")
     * @param organizationId テナントID
     * @param payload イベントペイロード（JSON にシリアライズされる）
     * @return 生成された event_id
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
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

        try {
            val metadata =
                OutgoingRabbitMQMetadata
                    .builder()
                    .withRoutingKey(eventType)
                    .withContentType("application/json")
                    .build()

            emitter.send(Message.of(json, Metadata.of(metadata)))
            log.infof("Published event %s (type=%s) to RabbitMQ", eventId, eventType)
        } catch (e: Exception) {
            log.warnf(
                e,
                "Failed to publish event %s (type=%s) to RabbitMQ, saving to outbox for retry",
                eventId,
                eventType,
            )
            saveToOutbox(eventType, json)
        }
        return eventId
    }

    /**
     * アウトボックステーブルにイベントを保存する。
     */
    private fun saveToOutbox(
        eventType: String,
        json: String,
    ) {
        val outboxEvent =
            OutboxEventEntity().apply {
                this.eventType = eventType
                this.payload = json
                this.status = "PENDING"
                this.retryCount = 0
            }
        outboxRepository.persist(outboxEvent)
        log.infof("Saved event to outbox: type=%s", eventType)
    }
}
