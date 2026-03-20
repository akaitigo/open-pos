package com.openpos.store.entity

import com.openpos.store.entity.annotation.PersonalData
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.PrePersist
import jakarta.persistence.Table
import org.hibernate.annotations.Filter
import org.hibernate.annotations.FilterDef
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.ParamDef
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.util.UUID

/**
 * 監査ログエンティティ。
 * 重要操作（作成・更新・削除）の記録を保存する。
 *
 * organizationFilter を定義し、テナント単位のアクセス制御を可能にする。
 * 管理者横断参照時はフィルターを有効化しないことで対応。
 */
@Entity
@Table(name = "audit_logs", schema = "store_schema")
@FilterDef(
    name = "organizationFilter",
    parameters = [ParamDef(name = "organizationId", type = UUID::class)],
)
@Filter(name = "organizationFilter", condition = "organization_id = :organizationId")
class AuditLogEntity {
    @Id
    @GeneratedValue
    lateinit var id: UUID

    @Column(name = "organization_id", nullable = false)
    lateinit var organizationId: UUID

    @Column(name = "staff_id")
    var staffId: UUID? = null

    @Column(name = "action", nullable = false, length = 50)
    lateinit var action: String

    @Column(name = "entity_type", nullable = false, length = 50)
    lateinit var entityType: String

    @Column(name = "entity_id", length = 255)
    var entityId: String? = null

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "details", nullable = false, columnDefinition = "jsonb")
    var details: String = "{}"

    /** @PII クライアント IP アドレス */
    @PersonalData(category = "IP_ADDRESS", description = "操作元IPアドレス（マスク済み）")
    @Column(name = "ip_address", length = 45)
    var ipAddress: String? = null

    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant = Instant.now()

    @PrePersist
    fun prePersist() {
        createdAt = Instant.now()
    }
}
