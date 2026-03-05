package com.openpos.inventory.event

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.jboss.logging.Logger
import java.time.Instant
import java.util.UUID

/**
 * 冪等性を保証するイベントハンドラーのベース。
 * event_id による重複排除を行い、at-least-once delivery を exactly-once 処理に変換する。
 */
@ApplicationScoped
class IdempotentEventHandler {
    @Inject
    lateinit var processedEventRepository: ProcessedEventRepository

    private val log = Logger.getLogger(IdempotentEventHandler::class.java)

    /**
     * イベントを冪等に処理する。
     * @param eventId イベントID（冪等性キー）
     * @param eventType イベントタイプ（ログ用）
     * @param action 実際の処理ロジック
     * @return true=処理実行, false=重複スキップ
     */
    @Transactional
    fun handleIdempotent(
        eventId: UUID,
        eventType: String,
        action: () -> Unit,
    ): Boolean {
        if (processedEventRepository.isProcessed(eventId)) {
            log.infof("Skipping duplicate event: %s (type=%s)", eventId, eventType)
            return false
        }

        action()

        val record =
            ProcessedEventEntity().apply {
                this.eventId = eventId
                this.eventType = eventType
                this.processedAt = Instant.now()
            }
        processedEventRepository.persist(record)

        log.infof("Processed event: %s (type=%s)", eventId, eventType)
        return true
    }
}
