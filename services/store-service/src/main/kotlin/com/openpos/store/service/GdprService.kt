package com.openpos.store.service

import com.openpos.store.entity.DataProcessingConsentEntity
import com.openpos.store.repository.AttendanceRepository
import com.openpos.store.repository.AuditLogRepository
import com.openpos.store.repository.CustomerRepository
import com.openpos.store.repository.DataProcessingConsentRepository
import com.openpos.store.repository.FavoriteProductRepository
import com.openpos.store.repository.GiftCardRepository
import com.openpos.store.repository.NotificationRepository
import com.openpos.store.repository.OrganizationRepository
import com.openpos.store.repository.PointTransactionRepository
import com.openpos.store.repository.ShiftRepository
import com.openpos.store.repository.StaffRepository
import com.openpos.store.repository.StoreRepository
import com.openpos.store.repository.SubscriptionRepository
import com.openpos.store.repository.SystemSettingRepository
import com.openpos.store.repository.TerminalRepository
import com.openpos.store.repository.WebhookRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID
import java.util.logging.Logger

/**
 * GDPR / 個人情報保護サービス。
 * テナント単位のデータ削除・PII 匿名化・同意管理を提供する。
 */
@ApplicationScoped
class GdprService {
    @Inject lateinit var organizationRepository: OrganizationRepository

    @Inject lateinit var storeRepository: StoreRepository

    @Inject lateinit var staffRepository: StaffRepository

    @Inject lateinit var customerRepository: CustomerRepository

    @Inject lateinit var terminalRepository: TerminalRepository

    @Inject lateinit var attendanceRepository: AttendanceRepository

    @Inject lateinit var shiftRepository: ShiftRepository

    @Inject lateinit var notificationRepository: NotificationRepository

    @Inject lateinit var favoriteProductRepository: FavoriteProductRepository

    @Inject lateinit var pointTransactionRepository: PointTransactionRepository

    @Inject lateinit var giftCardRepository: GiftCardRepository

    @Inject lateinit var webhookRepository: WebhookRepository

    @Inject lateinit var subscriptionRepository: SubscriptionRepository

    @Inject lateinit var systemSettingRepository: SystemSettingRepository

    @Inject lateinit var auditLogRepository: AuditLogRepository

    @Inject lateinit var consentRepository: DataProcessingConsentRepository

    companion object {
        private val logger: Logger = Logger.getLogger(GdprService::class.java.name)

        /** 匿名化に使う固定文字列 */
        const val ANONYMIZED_NAME = "[削除済み]"
        const val ANONYMIZED_EMAIL = "deleted@anonymized.invalid"
        const val ANONYMIZED_PHONE = "000-0000-0000"
        const val ANONYMIZED_ADDRESS = "[削除済み]"
    }

    /**
     * テナント（組織）の全データを論理削除する。
     * organizations テーブルの deleted_at を設定し、
     * 配下の PII フィールドを匿名化する。
     *
     * @param organizationId 削除対象の組織 ID
     * @return 削除が成功した場合 true
     * @throws IllegalArgumentException 組織が存在しない場合
     */
    @Transactional
    fun deleteOrganizationData(organizationId: UUID): Boolean {
        val org =
            organizationRepository.findByIdNotDeleted(organizationId)
                ?: throw IllegalArgumentException("Organization not found: $organizationId")

        logger.info("GDPR データ削除開始: organizationId=$organizationId")

        // 1. スタッフの PII を匿名化
        val staffList = staffRepository.findAllByOrganizationId(organizationId)
        staffList.forEach { staff ->
            staff.name = ANONYMIZED_NAME
            staff.email = null
            staff.pinHash = null
            staff.hydraSubject = null
            staff.isActive = false
            staffRepository.persist(staff)
        }
        logger.info("スタッフ匿名化完了: ${staffList.size} 件")

        // 2. 顧客の PII を匿名化
        val customers = customerRepository.findAllByOrganizationId(organizationId)
        customers.forEach { customer ->
            customer.name = ANONYMIZED_NAME
            customer.email = null
            customer.phone = null
            customer.isActive = false
            customerRepository.persist(customer)
        }
        logger.info("顧客匿名化完了: ${customers.size} 件")

        // 3. 店舗の PII を匿名化
        val stores = storeRepository.findAllByOrganizationId(organizationId)
        stores.forEach { store ->
            store.address = null
            store.phone = null
            store.isActive = false
            storeRepository.persist(store)
        }
        logger.info("店舗匿名化完了: ${stores.size} 件")

        // 4. 監査ログの IP アドレスをクリア
        val auditLogs = auditLogRepository.findAllByOrganizationId(organizationId)
        auditLogs.forEach { log ->
            log.ipAddress = null
            auditLogRepository.persist(log)
        }
        logger.info("監査ログ IP クリア完了: ${auditLogs.size} 件")

        // 5. 組織を論理削除
        org.deletedAt = Instant.now()
        organizationRepository.persist(org)

        logger.info("GDPR データ削除完了: organizationId=$organizationId")
        return true
    }

    /**
     * 完了済み取引に紐づくスタッフ名を匿名化する。
     * 指定された組織内で、全スタッフの PII をマスクする。
     *
     * @param organizationId 対象組織 ID
     * @return 匿名化されたスタッフ数
     */
    @Transactional
    fun anonymizeStaffData(organizationId: UUID): Int {
        val staffList = staffRepository.findAllByOrganizationId(organizationId)
        staffList.forEach { staff ->
            staff.name = ANONYMIZED_NAME
            staff.email = null
            staff.pinHash = null
            staff.hydraSubject = null
            staffRepository.persist(staff)
        }
        logger.info("スタッフデータ匿名化完了: organizationId=$organizationId, ${staffList.size} 件")
        return staffList.size
    }

    /**
     * 顧客データを匿名化する。
     *
     * @param organizationId 対象組織 ID
     * @return 匿名化された顧客数
     */
    @Transactional
    fun anonymizeCustomerData(organizationId: UUID): Int {
        val customers = customerRepository.findAllByOrganizationId(organizationId)
        customers.forEach { customer ->
            customer.name = ANONYMIZED_NAME
            customer.email = null
            customer.phone = null
            customerRepository.persist(customer)
        }
        logger.info("顧客データ匿名化完了: organizationId=$organizationId, ${customers.size} 件")
        return customers.size
    }

    // === 同意管理 ===

    /**
     * データ処理同意を記録する。
     *
     * @param organizationId 組織 ID
     * @param consentType 同意種別
     * @param granted 同意の有無
     * @param grantedBy 同意を付与したスタッフ ID
     * @param policyVersion プライバシーポリシーのバージョン
     * @param ipAddress 同意取得時の IP アドレス
     * @return 記録された同意エンティティ
     */
    @Transactional
    fun recordConsent(
        organizationId: UUID,
        consentType: String,
        granted: Boolean,
        grantedBy: UUID?,
        policyVersion: String,
        ipAddress: String?,
    ): DataProcessingConsentEntity {
        val existing = consentRepository.findByOrganizationAndType(organizationId, consentType)

        val entity =
            existing ?: DataProcessingConsentEntity().apply {
                this.organizationId = organizationId
                this.consentType = consentType
            }

        entity.granted = granted
        entity.policyVersion = policyVersion
        entity.grantedBy = grantedBy
        entity.ipAddress = ipAddress

        if (granted) {
            entity.grantedAt = Instant.now()
            entity.revokedAt = null
        } else {
            entity.revokedAt = Instant.now()
        }

        consentRepository.persist(entity)
        logger.info("同意記録: organizationId=$organizationId, type=$consentType, granted=$granted")
        return entity
    }

    /**
     * 組織の同意状況を取得する。
     *
     * @param organizationId 組織 ID
     * @return 同意エンティティのリスト
     */
    fun getConsents(organizationId: UUID): List<DataProcessingConsentEntity> = consentRepository.findByOrganizationId(organizationId)

    /**
     * 特定種別の同意を取得する。
     *
     * @param organizationId 組織 ID
     * @param consentType 同意種別
     * @return 同意エンティティ（未登録の場合は null）
     */
    fun getConsent(
        organizationId: UUID,
        consentType: String,
    ): DataProcessingConsentEntity? = consentRepository.findByOrganizationAndType(organizationId, consentType)
}
