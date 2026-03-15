package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * 通知エンティティ。
 * 通知システム (#174)。
 */
@Entity
@Table(name = "notifications", schema = "store_schema")
class NotificationEntity : BaseEntity() {
    @Column(name = "type", nullable = false, length = 50)
    lateinit var type: String

    @Column(name = "title", nullable = false, length = 255)
    lateinit var title: String

    @Column(name = "message", nullable = false, columnDefinition = "text")
    lateinit var message: String

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false
}
