package com.openpos.analytics.event

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * 処理済みイベントを記録するテーブル。
 * event_id による重複排除で冪等性を保証する。
 */
@Entity
@Table(name = "processed_events", schema = "analytics_schema")
class ProcessedEventEntity {
    @Id
    @Column(name = "event_id", nullable = false)
    lateinit var eventId: UUID

    @Column(name = "event_type", nullable = false, length = 50)
    lateinit var eventType: String

    @Column(name = "processed_at", nullable = false)
    var processedAt: Instant = Instant.now()
}
