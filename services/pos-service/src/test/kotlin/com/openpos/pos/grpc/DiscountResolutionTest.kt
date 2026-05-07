package com.openpos.pos.grpc

import com.openpos.pos.entity.TransactionEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import openpos.pos.v1.ApplyDiscountRequest
import java.util.UUID

class DiscountResolutionTest {
    private val organizationId = UUID.fromString("00000000-0000-0000-0000-000000000000")
    private val transactionId = UUID.fromString("11111111-1111-1111-1111-111111111111")
    private val discountId = UUID.fromString("22222222-2222-2222-2222-222222222222")

    @Test
    fun `resolves oneof coupon path and computes percentage amount from subtotal`() {
        val result =
            resolveDiscountApplication(
                request =
                    ApplyDiscountRequest
                        .newBuilder()
                        .setTransactionId(transactionId.toString())
                        .setApplyCouponCode("SPRING10")
                        .build(),
                transactionId = transactionId,
                transactionItemId = null,
                loadTransaction = { transaction(subtotal = 20000) },
                validateCoupon = { code, orgId ->
                    assertEquals("SPRING10", code)
                    assertEquals(organizationId, orgId)
                    CouponValidationResult(
                        isValid = true,
                        discountId = discountId,
                        discountName = "春セール",
                        discountType = "PERCENTAGE",
                        discountValue = "10",
                    )
                },
                getDiscount = { _, _ -> error("getDiscount should not be called") },
            )

        assertEquals(discountId, result.discountId)
        assertEquals("春セール", result.name)
        assertEquals(2000, result.amount)
    }

    @Test
    fun `falls back to legacy discount_id path`() {
        val result =
            resolveDiscountApplication(
                request =
                    ApplyDiscountRequest
                        .newBuilder()
                        .setTransactionId(transactionId.toString())
                        .setDiscountId(discountId.toString())
                        .build(),
                transactionId = transactionId,
                transactionItemId = UUID.fromString("33333333-3333-3333-3333-333333333333"),
                loadTransaction = { transaction(subtotal = 50000) },
                validateCoupon = { _, _ -> error("validateCoupon should not be called") },
                getDiscount = { id, orgId ->
                    assertEquals(discountId, id)
                    assertEquals(organizationId, orgId)
                    DiscountInfo(
                        name = "固定値引き",
                        discountType = "FIXED_AMOUNT",
                        value = "1500",
                    )
                },
            )

        assertEquals(1500, result.amount)
        assertEquals("固定値引き", result.name)
        assertEquals("FIXED_AMOUNT", result.discountType)
        assertEquals(UUID.fromString("33333333-3333-3333-3333-333333333333"), result.transactionItemId)
    }

    @Test
    fun `invalid coupon surfaces a validation error`() {
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                resolveDiscountApplication(
                    request =
                        ApplyDiscountRequest
                            .newBuilder()
                            .setTransactionId(transactionId.toString())
                            .setCouponCode("BAD")
                            .build(),
                    transactionId = transactionId,
                    transactionItemId = null,
                    loadTransaction = { transaction(subtotal = 10000) },
                    validateCoupon = { _, _ ->
                        CouponValidationResult(isValid = false, reason = "expired")
                    },
                    getDiscount = { _, _ -> error("getDiscount should not be called") },
                )
            }

        assertEquals("Coupon is not valid: expired", error.message)
    }

    @Test
    fun `missing discount source is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            resolveDiscountApplication(
                request =
                    ApplyDiscountRequest
                        .newBuilder()
                        .setTransactionId(transactionId.toString())
                        .build(),
                transactionId = transactionId,
                transactionItemId = null,
                loadTransaction = { transaction(subtotal = 10000) },
                validateCoupon = { _, _ -> error("validateCoupon should not be called") },
                getDiscount = { _, _ -> error("getDiscount should not be called") },
            )
        }
    }

    @Test
    fun `unknown discount type returns zero amount`() {
        assertEquals(0L, computeDiscountAmount("UNKNOWN", "10", 10000))
    }

    private fun transaction(subtotal: Long): TransactionEntity =
        TransactionEntity().apply {
            id = transactionId
            organizationId = this@DiscountResolutionTest.organizationId
            this.subtotal = subtotal
        }
}
