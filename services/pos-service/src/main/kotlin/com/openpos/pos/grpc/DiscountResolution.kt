package com.openpos.pos.grpc

import com.openpos.pos.entity.TransactionEntity
import openpos.pos.v1.ApplyDiscountRequest
import java.util.UUID

internal data class ResolvedDiscountApplication(
    val transactionId: UUID,
    val transactionItemId: UUID?,
    val discountId: UUID?,
    val name: String,
    val discountType: String,
    val value: String,
    val amount: Long,
)

private sealed interface DiscountSpecifier {
    data class Coupon(val code: String) : DiscountSpecifier

    data class DiscountId(val id: UUID) : DiscountSpecifier
}

internal fun resolveDiscountApplication(
    request: ApplyDiscountRequest,
    transactionId: UUID,
    transactionItemId: UUID?,
    loadTransaction: (UUID) -> TransactionEntity,
    validateCoupon: (String, UUID) -> CouponValidationResult,
    getDiscount: (UUID, UUID) -> DiscountInfo,
): ResolvedDiscountApplication {
    val tx = loadTransaction(transactionId)
    val specifier = request.resolveDiscountSpecifier()

    return when (specifier) {
        is DiscountSpecifier.Coupon -> {
            val couponResult = validateCoupon(specifier.code, tx.organizationId)
            if (!couponResult.isValid) {
                throw IllegalArgumentException("Coupon is not valid: ${couponResult.reason}")
            }

            ResolvedDiscountApplication(
                transactionId = transactionId,
                transactionItemId = transactionItemId,
                discountId = couponResult.discountId,
                name = couponResult.discountName,
                discountType = couponResult.discountType,
                value = couponResult.discountValue,
                amount = computeDiscountAmount(couponResult.discountType, couponResult.discountValue, tx.subtotal),
            )
        }

        is DiscountSpecifier.DiscountId -> {
            val discountInfo = getDiscount(specifier.id, tx.organizationId)
            ResolvedDiscountApplication(
                transactionId = transactionId,
                transactionItemId = transactionItemId,
                discountId = specifier.id,
                name = discountInfo.name,
                discountType = discountInfo.discountType,
                value = discountInfo.value,
                amount = computeDiscountAmount(discountInfo.discountType, discountInfo.value, tx.subtotal),
            )
        }
    }
}

internal fun computeDiscountAmount(
    discountType: String,
    discountValue: String,
    subtotal: Long,
): Long =
    when (discountType) {
        "PERCENTAGE" -> {
            val percentage = discountValue.toBigDecimal()
            (subtotal.toBigDecimal() * percentage / 100.toBigDecimal()).toLong()
        }

        "FIXED_AMOUNT" -> discountValue.toLong()

        else -> 0L
    }

private fun ApplyDiscountRequest.resolveDiscountSpecifier(): DiscountSpecifier =
    when {
        applyCouponCode.isNotBlank() -> DiscountSpecifier.Coupon(applyCouponCode)
        applyDiscountId.isNotBlank() -> DiscountSpecifier.DiscountId(UUID.fromString(applyDiscountId))
        couponCode.isNotBlank() -> DiscountSpecifier.Coupon(couponCode)
        discountId.isNotBlank() -> DiscountSpecifier.DiscountId(UUID.fromString(discountId))
        else -> throw IllegalArgumentException("Either coupon_code or discount_id must be specified")
    }
