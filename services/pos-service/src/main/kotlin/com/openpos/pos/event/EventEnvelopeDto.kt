package com.openpos.pos.event

/**
 * RabbitMQ メッセージボディの JSON 構造。
 * Proto の EventEnvelope と対応する。
 */
data class EventEnvelopeDto(
    val eventId: String,
    val eventType: String,
    val timestamp: String,
    val organizationId: String,
    val payload: String,
    val source: String,
)
