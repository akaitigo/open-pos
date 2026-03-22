package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CouponEntity
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.CouponRepository
import com.openpos.product.repository.DiscountRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * クーポン検証結果を保持するデータクラス。
 */
data class CouponValidationResult(
    val isValid: Boolean,
    val coupon: CouponEntity? = null,
    val discount: DiscountEntity? = null,
    val reason: String? = null,
)

/**
 * クーポンのビジネスロジック層。
 * CRUD とバリデーション（有効期間・利用回数チェック）を提供する。
 */
@ApplicationScoped
class CouponService {
    @Inject
    lateinit var couponRepository: CouponRepository

    @Inject
    lateinit var discountRepository: DiscountRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * クーポンを作成する。
     */
    @Transactional
    fun create(
        code: String,
        discountId: UUID,
        maxUses: Int?,
        validFrom: Instant?,
        validUntil: Instant?,
    ): CouponEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            CouponEntity().apply {
                this.organizationId = orgId
                this.code = code
                this.discountId = discountId
                this.maxUses = maxUses
                this.usedCount = 0
                this.validFrom = validFrom
                this.validUntil = validUntil
                this.isActive = true
            }
        couponRepository.persist(entity)
        return entity
    }

    /**
     * クーポンコードで検索する。
     */
    fun findByCode(code: String): CouponEntity? {
        tenantFilterService.enableFilter()
        return couponRepository.findByCode(code)
    }

    /**
     * IDでクーポンを取得する。
     */
    fun findById(id: UUID): CouponEntity? {
        tenantFilterService.enableFilter()
        return couponRepository.findById(id)
    }

    /**
     * クーポンコードを検証する。
     * 有効期間・利用回数をチェックし、結果を返す。
     */
    fun validate(code: String): CouponValidationResult {
        tenantFilterService.enableFilter()
        val coupon =
            couponRepository.findByCode(code)
                ?: return CouponValidationResult(isValid = false, reason = "NOT_FOUND")

        if (!coupon.isActive) {
            return CouponValidationResult(isValid = false, coupon = coupon, reason = "COUPON_INACTIVE")
        }

        val now = Instant.now()

        // 有効期間チェック
        coupon.validFrom?.let { from ->
            if (now.isBefore(from)) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "NOT_YET_VALID")
            }
        }
        coupon.validUntil?.let { until ->
            if (now.isAfter(until)) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "EXPIRED")
            }
        }

        // 利用回数チェック
        coupon.maxUses?.let { max ->
            if (coupon.usedCount >= max) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "MAX_USES_REACHED")
            }
        }

        // 紐付き割引の有効性チェック
        val discount = discountRepository.findById(coupon.discountId)
        if (discount == null || !discount.isActive) {
            return CouponValidationResult(isValid = false, coupon = coupon, reason = "DISCOUNT_INACTIVE")
        }

        return CouponValidationResult(isValid = true, coupon = coupon, discount = discount)
    }

    /**
     * クーポンを使用済みとしてマークし、usedCount をインクリメントする。
     * 悲観的ロック（SELECT FOR UPDATE）で並行利用を防止する。
     */
    @Transactional
    fun redeem(code: String): CouponValidationResult {
        tenantFilterService.enableFilter()
        val coupon =
            couponRepository.findByCodeForUpdate(code)
                ?: return CouponValidationResult(isValid = false, reason = "NOT_FOUND")

        if (!coupon.isActive) {
            return CouponValidationResult(isValid = false, coupon = coupon, reason = "COUPON_INACTIVE")
        }

        val now = Instant.now()

        coupon.validFrom?.let { from ->
            if (now.isBefore(from)) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "NOT_YET_VALID")
            }
        }
        coupon.validUntil?.let { until ->
            if (now.isAfter(until)) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "EXPIRED")
            }
        }

        coupon.maxUses?.let { max ->
            if (coupon.usedCount >= max) {
                return CouponValidationResult(isValid = false, coupon = coupon, reason = "MAX_USES_REACHED")
            }
        }

        val discount = discountRepository.findById(coupon.discountId)
        if (discount == null || !discount.isActive) {
            return CouponValidationResult(isValid = false, coupon = coupon, reason = "DISCOUNT_INACTIVE")
        }

        coupon.usedCount += 1
        couponRepository.persist(coupon)

        return CouponValidationResult(isValid = true, coupon = coupon, discount = discount)
    }
}
