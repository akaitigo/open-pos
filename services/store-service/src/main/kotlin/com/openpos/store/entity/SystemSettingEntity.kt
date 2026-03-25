package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

/**
 * システム設定エンティティ。
 * テナント（組織）レベルのキーバリュー設定を保持する。
 */
@Entity
@Table(name = "system_settings", schema = "store_schema")
class SystemSettingEntity : BaseEntity() {
    @Column(name = "\"key\"", nullable = false, length = 100)
    lateinit var key: String

    @Column(name = "\"value\"", nullable = false, columnDefinition = "text")
    var value: String = ""

    @Column(name = "description", length = 500)
    var description: String? = null
}
