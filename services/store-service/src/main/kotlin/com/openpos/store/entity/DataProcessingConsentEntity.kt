package com.openpos.store.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

/**
 * データ処理同意エンティティ。
 * GDPR / 個人情報保護法に基づく同意管理を行う。
 * テナント（組織）単位でデータ処理への同意を追跡する。
 *
 * organizations テーブルを参照するため BaseEntity を継承しない（organization_id フィルタ不要）。
 */
@Entity
@Table(name = "data_processing_consents", schema = "store_schema")
class DataProcessingConsentEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    /** 対象組織 */
    @Column(name = "organization_id", nullable = false)
    lateinit var organizationId: UUID

    /** 同意の種別（例: "DATA_PROCESSING", "MARKETING", "ANALYTICS"） */
    @Column(name = "consent_type", nullable = false, length = 50)
    lateinit var consentType: String

    /** 同意ステータス（true = 同意済み, false = 同意拒否/撤回） */
    @Column(name = "granted", nullable = false)
    var granted: Boolean = false

    /** 同意を付与した日時 */
    @Column(name = "granted_at")
    var grantedAt: Instant? = null

    /** 同意を撤回した日時 */
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null

    /** 同意を付与/撤回したスタッフID */
    @Column(name = "granted_by")
    var grantedBy: UUID? = null

    /** 同意バージョン（プライバシーポリシーのバージョン番号） */
    @Column(name = "policy_version", nullable = false, length = 20)
    var policyVersion: String = "1.0"

    /** IPアドレス（同意取得時の記録用、マスク済み） */
    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant = Instant.now()

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
        updatedAt = Instant.now()
    }

    @PreUpdate
    fun preUpdate() {
        updatedAt = Instant.now()
    }
}
