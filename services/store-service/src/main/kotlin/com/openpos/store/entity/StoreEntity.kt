package com.openpos.store.entity

import com.openpos.store.entity.annotation.PersonalData
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes

/**
 * 店舗エンティティ。
 * 組織配下の実店舗を表す。settings は JSONB で店舗固有設定を保持する。
 */
@Entity
@Table(name = "stores", schema = "store_schema")
class StoreEntity : BaseEntity() {
    @Column(name = "code", length = 20)
    var code: String? = null

    @Column(name = "name", nullable = false, length = 255)
    lateinit var name: String

    /** @PII 店舗所在地 */
    @PersonalData(category = "ADDRESS", description = "店舗住所")
    @Column(name = "address")
    var address: String? = null

    /** @PII 店舗電話番号 */
    @PersonalData(category = "PHONE", description = "店舗電話番号")
    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "timezone", nullable = false, length = 50)
    var timezone: String = "Asia/Tokyo"

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "settings", nullable = false, columnDefinition = "jsonb")
    var settings: String = "{}"

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
