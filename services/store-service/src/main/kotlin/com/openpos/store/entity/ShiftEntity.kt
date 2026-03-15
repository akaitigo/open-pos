package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

/**
 * シフトエンティティ。
 * シフト管理 (#172)。
 */
@Entity
@Table(name = "shifts", schema = "store_schema")
class ShiftEntity : BaseEntity() {
    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "date", nullable = false)
    lateinit var date: LocalDate

    @Column(name = "start_time", nullable = false)
    lateinit var startTime: LocalTime

    @Column(name = "end_time", nullable = false)
    lateinit var endTime: LocalTime

    @Column(name = "note")
    var note: String? = null
}
