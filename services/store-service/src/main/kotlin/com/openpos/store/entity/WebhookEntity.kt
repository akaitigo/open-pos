package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * Webhookエンティティ。
 * 外部サービスへのイベント通知設定を管理する。
 * events は JSON 配列で対象イベント種別を保持する。
 * secret は HMAC-SHA256 署名に使用する。
 */
@Entity
@Table(name = "webhooks", schema = "store_schema")
class WebhookEntity : BaseEntity() {
    @Column(name = "url", nullable = false, length = 2048)
    lateinit var url: String

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "events", nullable = false, columnDefinition = "jsonb")
    var events: String = "[]"

    @Column(name = "secret", nullable = false, length = 255)
    lateinit var secret: String

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
