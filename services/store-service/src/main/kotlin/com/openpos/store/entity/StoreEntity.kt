package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

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

    @Column(name = "address")
    var address: String? = null

    @Column(name = "phone", length = 20)
    var phone: String? = null

    @Column(name = "timezone", nullable = false, length = 50)
    var timezone: String = "Asia/Tokyo"

    @Column(name = "settings", nullable = false, columnDefinition = "TEXT")
    var settings: String = "{}"

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true
}
