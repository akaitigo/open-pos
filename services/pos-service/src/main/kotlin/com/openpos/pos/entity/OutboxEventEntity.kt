package com.openpos.pos.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * イベントアウトボックスエンティティ。
 * RabbitMQ への送信が失敗した場合にイベントを一時保存し、
 * OutboxProcessor によるリトライ送信を実現する。
 */
@Entity
@Table(name = "outbox_events", schema = "pos_schema")
class OutboxEventEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "event_type", nullable = false)
    lateinit var eventType: String

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    lateinit var payload: String

    @Column(name = "status", nullable = false)
    var status: String = "PENDING"

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "sent_at")
    var sentAt: Instant? = null

    @Column(name = "retry_count", nullable = false)
    var retryCount: Int = 0

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        // no-op: updated fields are managed explicitly
    }
}
