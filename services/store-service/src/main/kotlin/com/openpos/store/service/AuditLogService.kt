package com.openpos.store.service

import com.openpos.store.entity.AuditLogEntity
import com.openpos.store.repository.AuditLogRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID
import java.util.logging.Logger

/**
 * 監査ログサービス。
 * 重要操作を audit_logs テーブルに記録する。
 * PII（個人情報）はマスクしてから渡すこと。
 */
@ApplicationScoped
class AuditLogService {
    @Inject
    lateinit var auditLogRepository: AuditLogRepository

    companion object {
        private val logger: Logger = Logger.getLogger(AuditLogService::class.java.name)
    }

    /**
     * 監査ログを記録する。
     *
     * @param organizationId テナントID
     * @param staffId 操作者のスタッフID（不明な場合は null）
     * @param action 操作種別（CREATE, UPDATE, DELETE, LOGIN, LOGOUT 等）
     * @param entityType エンティティ種別（STAFF, STORE, PRODUCT 等）
     * @param entityId 対象エンティティID
     * @param details 追加情報（JSON 文字列）。PII を含めないこと。
     * @param ipAddress クライアントIPアドレス
     */
    @Transactional
    fun log(
        organizationId: UUID,
        staffId: UUID? = null,
        action: String,
        entityType: String,
        entityId: String? = null,
        details: String = "{}",
        ipAddress: String? = null,
    ) {
        val entity =
            AuditLogEntity().apply {
                this.organizationId = organizationId
                this.staffId = staffId
                this.action = action
                this.entityType = entityType
                this.entityId = entityId
                this.details = details
                this.ipAddress = maskIpAddress(ipAddress)
            }
        auditLogRepository.persist(entity)
        logger.fine { "Audit: $action $entityType ${entityId ?: "(none)"} by ${staffId ?: "system"}" }
    }

    /**
     * IP アドレスの最終オクテットをマスクする（プライバシー保護）。
     * 例: 192.168.1.100 → 192.168.1.***
     */
    private fun maskIpAddress(ipAddress: String?): String? {
        if (ipAddress == null) return null
        // IPv4
        val ipv4Regex = Regex("""^(\d+\.\d+\.\d+\.)\d+$""")
        ipv4Regex.find(ipAddress)?.let { match ->
            return "${match.groupValues[1]}***"
        }
        // IPv6 or other — mask last segment
        val lastColon = ipAddress.lastIndexOf(':')
        if (lastColon > 0) {
            return ipAddress.substring(0, lastColon + 1) + "***"
        }
        return ipAddress
    }
}
