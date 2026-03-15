package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * 勤怠エンティティ。
 * 打刻機能 (#171)。
 */
@Entity
@Table(name = "attendances", schema = "store_schema")
class AttendanceEntity : BaseEntity() {
    @Column(name = "staff_id", nullable = false)
    lateinit var staffId: UUID

    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    @Column(name = "date", nullable = false)
    lateinit var date: LocalDate

    @Column(name = "clock_in", nullable = false)
    lateinit var clockIn: Instant

    @Column(name = "clock_out")
    var clockOut: Instant? = null
}
