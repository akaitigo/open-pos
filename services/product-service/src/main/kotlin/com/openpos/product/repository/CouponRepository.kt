package com.openpos.product.repository

import com.openpos.product.entity.CouponEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.LockModeType
import java.time.Instant
import java.util.UUID

/**
 * クーポンリポジトリ。
 */
@ApplicationScoped
class CouponRepository : PanacheRepositoryBase<CouponEntity, UUID> {
    /**
     * クーポンコードでクーポンを検索する（組織フィルター適用後に一意）。
     */
    fun findByCode(code: String): CouponEntity? = find("code = ?1", code).firstResult()

    /**
     * クーポンコードで検索し、悲観的ロック（SELECT FOR UPDATE）を取得する。
     * 並行利用を防止するために redeem() で使用。
     */
    fun findByCodeForUpdate(code: String): CouponEntity? = find("code = ?1", code).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()

    /**
     * 有効かつ期間内のクーポンのみ取得する。
     */
    fun findActiveAndValid(now: Instant): List<CouponEntity> =
        list(
            "isActive = true AND (validFrom IS NULL OR validFrom <= ?1) AND (validUntil IS NULL OR validUntil >= ?1) ORDER BY code ASC",
            now,
        )

    /**
     * 全クーポンを取得する。
     */
    fun findAllOrdered(): List<CouponEntity> = list("ORDER BY code ASC")
}
