package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * POS 端末エンティティ。
 * terminal_code は人間向けの識別コード（例: "POS-01"）。
 */
@Entity
@Table(name = "terminals", schema = "store_schema")
class TerminalEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "terminal_code", nullable = false, length = 20)
    lateinit var terminalCode: String

    @Column(name = "name", length = 100)
    var name: String? = null

    @Column(name = "last_sync_at")
    var lastSyncAt: Instant? = null

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
