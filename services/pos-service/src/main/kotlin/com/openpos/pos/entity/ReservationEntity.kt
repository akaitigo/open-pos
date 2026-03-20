package com.openpos.pos.entity

import com.openpos.pos.entity.annotation.PersonalData
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * 予約注文エンティティ。
 * 顧客の取り置き・予約注文を管理する。
 * items は JSON 配列で予約商品を保持する。
 * ステータス遷移: RESERVED -> FULFILLED / CANCELLED / EXPIRED
 */
@Entity
@Table(name = "reservations", schema = "pos_schema")
class ReservationEntity : BaseEntity() {
    @Column(name = "store_id", nullable = false)
    lateinit var storeId: UUID

    /** @PII 予約顧客名 */
    @PersonalData(category = "NAME", description = "予約顧客氏名")
    @Column(name = "customer_name", length = 255)
    var customerName: String? = null

    /** @PII 予約顧客電話番号 */
    @PersonalData(category = "PHONE", description = "予約顧客電話番号")
    @Column(name = "customer_phone", length = 20)
    var customerPhone: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "items", nullable = false, columnDefinition = "jsonb")
    var items: String = "[]"

    @Column(name = "reserved_until", nullable = false)
    lateinit var reservedUntil: Instant

    @Column(name = "status", nullable = false, length = 20)
    var status: String = "RESERVED"

    @Column(name = "note")
    var note: String? = null
}
