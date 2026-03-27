package com.openpos.pos.event

import com.openpos.pos.entity.OutboxEventEntity
import com.openpos.pos.repository.OutboxRepository
import io.quarkus.scheduler.Scheduled
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

/**
 * アウトボックスプロセッサ。
 * 定期的に PENDING イベントを取得し、RabbitMQ への再送信を試みる。
 * 最大リトライ回数（10回）を超えた場合は FAILED に遷移する。
 *
 * 二重パブリッシュ防止:
 * SELECT FOR UPDATE → IN_PROGRESS 更新を同一トランザクション内で実行し、
 * 他インスタンスが同じイベントを取得できないようにする。
 */
@ApplicationScoped
class OutboxProcessor {
    @Inject
    @Channel("outgoing-events")
    lateinit var emitter: Emitter<String>

    @Inject
    lateinit var outboxRepository: OutboxRepository

    private val log = Logger.getLogger(OutboxProcessor::class.java)

    companion object {
        const val MAX_RETRY_COUNT = 10
        const val BATCH_SIZE = 100
    }

    /**
     * 10秒ごとに PENDING イベントを処理する。
     * バッチサイズ100件で高スループット環境での遅延を削減。
     */
    @Scheduled(every = "10s", identity = "outbox-processor")
    fun processOutbox() {
        val claimedEvents = claimPendingEvents()
        if (claimedEvents.isEmpty()) {
            return
        }

        log.infof("Processing %d claimed outbox events", claimedEvents.size)

        for (event in claimedEvents) {
            processEvent(event.id.toString(), event.eventType, event.payload, event.retryCount)
        }
    }

    /**
     * PENDING イベントを取得し IN_PROGRESS に遷移する。
     * SELECT FOR UPDATE とステータス更新が同一トランザクション内で実行され、
     * 複数インスタンス間での二重取得を防止する。
     */
    @Transactional
    fun claimPendingEvents(): List<OutboxEventEntity> = outboxRepository.findPendingAndMarkInProgress(BATCH_SIZE)

    /**
     * 個別のアウトボックスイベントを処理する。
     * 各イベントは独立したトランザクションで処理する。
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    fun processEvent(
        eventId: String,
        eventType: String,
        payload: String,
        currentRetryCount: Int,
    ) {
        val event = outboxRepository.findById(java.util.UUID.fromString(eventId)) ?: return

        try {
            val metadata =
                OutgoingRabbitMQMetadata
                    .builder()
                    .withRoutingKey(eventType)
                    .withContentType("application/json")
                    .build()

            emitter.send(Message.of(payload, Metadata.of(metadata)))

            event.status = "SENT"
            event.sentAt = Instant.now()
            outboxRepository.persist(event)
            log.infof("Successfully sent outbox event %s (type=%s)", eventId, eventType)
        } catch (e: Exception) {
            event.retryCount = currentRetryCount + 1

            if (event.retryCount >= MAX_RETRY_COUNT) {
                event.status = "FAILED"
                log.errorf(
                    e,
                    "Outbox event %s (type=%s) exceeded max retries (%d), marked as FAILED",
                    eventId,
                    eventType,
                    MAX_RETRY_COUNT,
                )
            } else {
                event.status = "PENDING"
                log.warnf(
                    e,
                    "Failed to send outbox event %s (type=%s), retry %d/%d",
                    eventId,
                    eventType,
                    event.retryCount,
                    MAX_RETRY_COUNT,
                )
            }
            outboxRepository.persist(event)
        }
    }
}
