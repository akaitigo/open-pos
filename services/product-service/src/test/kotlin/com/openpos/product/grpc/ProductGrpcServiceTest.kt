package com.openpos.product.grpc

import com.google.protobuf.BoolValue
import com.google.protobuf.Int32Value
import com.google.protobuf.Int64Value
import com.openpos.product.entity.CouponEntity
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.entity.ProductEntity
import com.openpos.product.entity.TaxRateEntity
import com.openpos.product.service.CategoryService
import com.openpos.product.service.CouponService
import com.openpos.product.service.CouponValidationResult
import com.openpos.product.service.DiscountService
import com.openpos.product.service.ProductService
import com.openpos.product.service.TaxRateService
import io.grpc.stub.StreamObserver
import openpos.product.v1.CreateProductRequest
import openpos.product.v1.DiscountType
import openpos.product.v1.UpdateDiscountRequest
import openpos.product.v1.UpdateProductRequest
import openpos.product.v1.UpdateTaxRateRequest
import openpos.product.v1.ValidateCouponRequest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

class ProductGrpcServiceTest {
    private lateinit var grpcService: ProductGrpcService
    private val productService = mock<ProductService>()
    private val categoryService = mock<CategoryService>()
    private val taxRateService = mock<TaxRateService>()
    private val discountService = mock<DiscountService>()
    private val couponService = mock<CouponService>()
    private val tenantHelper = mock<GrpcTenantHelper>()

    @BeforeEach
    fun setUp() {
        grpcService =
            ProductGrpcService().apply {
                this.productService = this@ProductGrpcServiceTest.productService
                this.categoryService = this@ProductGrpcServiceTest.categoryService
                this.taxRateService = this@ProductGrpcServiceTest.taxRateService
                this.discountService = this@ProductGrpcServiceTest.discountService
                this.couponService = this@ProductGrpcServiceTest.couponService
                this.tenantHelper = this@ProductGrpcServiceTest.tenantHelper
            }
    }

    @Test
    fun `createProduct forwards description to service`() {
        val productId = UUID.randomUUID()
        whenever(
            productService.create(
                eq("コーヒー"),
                eq("豆から抽出したホットコーヒー"),
                anyOrNull(),
                anyOrNull(),
                eq(480L),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(0),
            ),
        ).thenReturn(productEntity(productId))

        val observer = CapturingObserver<openpos.product.v1.CreateProductResponse>()

        grpcService.createProduct(
            CreateProductRequest
                .newBuilder()
                .setName("コーヒー")
                .setDescription("豆から抽出したホットコーヒー")
                .setPrice(480L)
                .build(),
            observer,
        )

        verify(tenantHelper).setupTenantContextWithoutFilter()
        verify(productService).create(
            eq("コーヒー"),
            eq("豆から抽出したホットコーヒー"),
            anyOrNull(),
            anyOrNull(),
            eq(480L),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(0),
        )
        assertNotNull(observer.value)
        assertEquals("商品説明", observer.value!!.product.description)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateProduct forwards explicit zero and false values`() {
        val productId = UUID.randomUUID()
        whenever(
            productService.update(
                eq(productId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(0L),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(0),
                eq(false),
            ),
        ).thenReturn(productEntity(productId))

        val observer = CapturingObserver<openpos.product.v1.UpdateProductResponse>()

        grpcService.updateProduct(
            UpdateProductRequest
                .newBuilder()
                .setId(productId.toString())
                .setPriceValue(Int64Value.of(0))
                .setDisplayOrderValue(Int32Value.of(0))
                .setIsActiveValue(BoolValue.of(false))
                .build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        verify(productService).update(
            eq(productId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(0L),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(0),
            eq(false),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateProduct keeps presence fields null when omitted`() {
        val productId = UUID.randomUUID()
        whenever(
            productService.update(
                eq(productId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                isNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                isNull(),
                isNull(),
            ),
        ).thenReturn(productEntity(productId))

        val observer = CapturingObserver<openpos.product.v1.UpdateProductResponse>()

        grpcService.updateProduct(
            UpdateProductRequest.newBuilder().setId(productId.toString()).build(),
            observer,
        )

        verify(productService).update(
            eq(productId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            isNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            isNull(),
            isNull(),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateTaxRate forwards explicit false booleans`() {
        val taxRateId = UUID.randomUUID()
        whenever(
            taxRateService.update(
                eq(taxRateId),
                anyOrNull(),
                anyOrNull(),
                eq("STANDARD"),
                isNull(),
                eq(false),
            ),
        ).thenReturn(taxRateEntity(taxRateId))

        val observer = CapturingObserver<openpos.product.v1.UpdateTaxRateResponse>()

        grpcService.updateTaxRate(
            UpdateTaxRateRequest
                .newBuilder()
                .setId(taxRateId.toString())
                .setIsReducedValue(BoolValue.of(false))
                .setIsDefaultValue(BoolValue.of(false))
                .build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        verify(taxRateService).update(
            eq(taxRateId),
            anyOrNull(),
            anyOrNull(),
            eq("STANDARD"),
            isNull(),
            eq(false),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `updateDiscount forwards explicit false isActive`() {
        val discountId = UUID.randomUUID()
        whenever(
            discountService.update(
                eq(discountId),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                anyOrNull(),
                eq(false),
            ),
        ).thenReturn(discountEntity(discountId))

        val observer = CapturingObserver<openpos.product.v1.UpdateDiscountResponse>()

        grpcService.updateDiscount(
            UpdateDiscountRequest
                .newBuilder()
                .setId(discountId.toString())
                .setDiscountType(DiscountType.DISCOUNT_TYPE_PERCENTAGE)
                .setIsActiveValue(BoolValue.of(false))
                .build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        verify(discountService).update(
            eq(discountId),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            anyOrNull(),
            eq(false),
        )
        assertNotNull(observer.value)
        assertTrue(observer.completed)
    }

    @Test
    fun `validateCoupon returns discount payload when coupon is valid`() {
        val couponId = UUID.randomUUID()
        val discountId = UUID.randomUUID()
        whenever(couponService.validate("WELCOME")).thenReturn(
            CouponValidationResult(
                isValid = true,
                coupon = couponEntity(couponId, discountId),
                discount = discountEntity(discountId),
            ),
        )

        val observer = CapturingObserver<openpos.product.v1.ValidateCouponResponse>()

        grpcService.validateCoupon(
            ValidateCouponRequest.newBuilder().setCode("WELCOME").build(),
            observer,
        )

        verify(tenantHelper).setupTenantContext()
        assertNotNull(observer.value)
        assertTrue(observer.value!!.isValid)
        assertTrue(observer.value!!.hasDiscount())
        assertTrue(observer.value!!.coupon.isActive)
        assertFalse(observer.value!!.discount.value.isBlank())
        assertTrue(observer.completed)
    }

    private fun productEntity(productId: UUID): ProductEntity =
        ProductEntity().apply {
            id = productId
            organizationId = UUID.randomUUID()
            name = "コーヒー"
            description = "商品説明"
            price = 480L
            displayOrder = 0
            isActive = true
        }

    private fun taxRateEntity(taxRateId: UUID): TaxRateEntity =
        TaxRateEntity().apply {
            id = taxRateId
            organizationId = UUID.randomUUID()
            name = "標準税率10%"
            rate = BigDecimal("0.1000")
            taxType = "STANDARD"
            isDefault = false
            isActive = true
        }

    private fun discountEntity(discountId: UUID): DiscountEntity =
        DiscountEntity().apply {
            id = discountId
            organizationId = UUID.randomUUID()
            name = "10% OFF"
            discountType = "PERCENTAGE"
            value = 10
            isActive = true
        }

    private fun couponEntity(
        couponId: UUID,
        discountId: UUID,
    ): CouponEntity =
        CouponEntity().apply {
            id = couponId
            organizationId = UUID.randomUUID()
            code = "WELCOME"
            this.discountId = discountId
            maxUses = 100
            usedCount = 1
            validFrom = Instant.now().minusSeconds(3600)
            validUntil = Instant.now().plusSeconds(3600)
            isActive = true
        }

    private class CapturingObserver<T> : StreamObserver<T> {
        var value: T? = null
        var completed = false

        override fun onNext(value: T) {
            this.value = value
        }

        override fun onError(t: Throwable) {
            throw AssertionError("Unexpected error", t)
        }

        override fun onCompleted() {
            completed = true
        }
    }
}
