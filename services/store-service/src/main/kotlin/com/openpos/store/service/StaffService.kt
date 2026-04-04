package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StaffEntity
import com.openpos.store.repository.StaffRepository
import com.openpos.store.repository.StoreRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

data class PinAuthResult(
    val success: Boolean,
    val staff: StaffEntity? = null,
    val reason: String? = null,
)

@ApplicationScoped
class StaffService {
    @Inject
    lateinit var staffRepository: StaffRepository

    @Inject
    lateinit var storeRepository: StoreRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    companion object {
        private const val MAX_PIN_FAILURES = 5
        private const val LOCK_DURATION_MINUTES = 30L

        // タイミングサイドチャネル対策用ダミーハッシュ（bcrypt cost=12）
        private const val DUMMY_BCRYPT_HASH = "\$2a\$12\$LJ3m4ys4yz0z4H7kZ2V6OuQ1q1q1q1q1q1q1q1q1q1q1q1q1q1q1q"
    }

    @Transactional
    fun create(
        storeId: UUID,
        name: String,
        email: String,
        role: String,
        pinHash: String,
    ): StaffEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        // storeId が自組織に属するか検証
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        val store =
            storeRepository.find("id = ?1", storeId).firstResult()
                ?: throw IllegalArgumentException("Store $storeId not found in organization $orgId")
        require(store.organizationId == orgId) {
            "Store $storeId does not belong to organization $orgId"
        }
        val entity =
            StaffEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.name = name
                this.email = email
                this.role = role
                this.pinHash = pinHash
                this.isActive = true
            }
        staffRepository.persist(entity)
        return entity
    }

    @Transactional
    fun findById(id: UUID): StaffEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        return staffRepository.find("id = ?1", id).firstResult()
    }

    @Transactional
    fun listByStoreId(
        storeId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<StaffEntity>, Long> {
        tenantFilterService.enableFilter()
        val staff = staffRepository.findByStoreId(storeId, Page.of(page, pageSize))
        val totalCount = staffRepository.countByStoreId(storeId)
        return Pair(staff, totalCount)
    }

    @Transactional
    fun update(
        id: UUID,
        name: String?,
        email: String?,
        role: String?,
        pinHash: String?,
        isActive: Boolean?,
    ): StaffEntity? {
        tenantFilterService.enableFilter()
        val entity = staffRepository.find("id = ?1", id).firstResult() ?: return null
        name?.let { entity.name = it }
        email?.let { entity.email = it }
        role?.let { entity.role = it }
        pinHash?.let { entity.pinHash = it }
        isActive?.let { entity.isActive = it }
        staffRepository.persist(entity)
        return entity
    }

    /**
     * PIN 認証を行う。
     * pinVerifier は bcrypt 検証関数をコールバックとして受け取る（DI の bcrypt 依存を避ける）。
     * 悲観的ロック（SELECT FOR UPDATE）で pinFailedCount の read-modify-write を保護する。
     */
    @Transactional
    fun authenticateByPin(
        staffId: UUID,
        storeId: UUID,
        pin: String,
        pinVerifier: (String, String) -> Boolean,
    ): PinAuthResult {
        tenantFilterService.enableFilter()
        val staff = staffRepository.findByIdForUpdate(staffId)
        if (staff == null || staff.storeId != storeId) {
            // タイミングサイドチャネル対策: ダミー bcrypt 検証で応答時間を均一化
            pinVerifier("dummy", DUMMY_BCRYPT_HASH)
            return PinAuthResult(false, reason = "NOT_FOUND")
        }

        if (!staff.isActive) {
            pinVerifier("dummy", DUMMY_BCRYPT_HASH)
            return PinAuthResult(false, staff = staff, reason = "ACCOUNT_INACTIVE")
        }

        // ロック状態チェック
        staff.pinLockedUntil?.let { lockedUntil ->
            if (Instant.now().isBefore(lockedUntil)) {
                return PinAuthResult(false, staff = staff, reason = "ACCOUNT_LOCKED")
            }
            // ロック期間終了: リセット
            staff.pinFailedCount = 0
            staff.pinLockedUntil = null
        }

        val storedHash = staff.pinHash ?: return PinAuthResult(false, staff = staff, reason = "PIN_NOT_SET")

        if (!pinVerifier(pin, storedHash)) {
            staff.pinFailedCount++
            if (staff.pinFailedCount >= MAX_PIN_FAILURES) {
                staff.pinLockedUntil = Instant.now().plusSeconds(LOCK_DURATION_MINUTES * 60)
            }
            staffRepository.persist(staff)
            return PinAuthResult(false, staff = staff, reason = "INVALID_PIN")
        }

        // 成功: リセット
        staff.pinFailedCount = 0
        staff.pinLockedUntil = null
        staffRepository.persist(staff)
        return PinAuthResult(true, staff = staff)
    }
}
