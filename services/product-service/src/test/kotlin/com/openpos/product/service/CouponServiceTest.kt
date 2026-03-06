package com.openpos.product.service

import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.CouponEntity
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.CouponRepository
import com.openpos.product.repository.DiscountRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@QuarkusTest
class CouponServiceTest {
    @Inject
    lateinit var couponService: CouponService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @InjectMock
    lateinit var couponRepository: CouponRepository

    @InjectMock
    lateinit var discountRepository: DiscountRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    private val orgId = UUID.randomUUID()
    private val discountId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `クーポンを正常に作成する`() {
            // Arrange
            val validFrom = Instant.now()
            val validUntil = Instant.now().plus(30, ChronoUnit.DAYS)
            doNothing().whenever(couponRepository).persist(any<CouponEntity>())

            // Act
            val result =
                couponService.create(
                    code = "SUMMER2026",
                    discountId = discountId,
                    maxUses = 100,
                    validFrom = validFrom,
                    validUntil = validUntil,
                )

            // Assert
            assertEquals("SUMMER2026", result.code)
            assertEquals(discountId, result.discountId)
            assertEquals(100, result.maxUses)
            assertEquals(0, result.usedCount)
            assertEquals(validFrom, result.validFrom)
            assertEquals(validUntil, result.validUntil)
            assertEquals(orgId, result.organizationId)
            verify(couponRepository).persist(any<CouponEntity>())
        }

        @Test
        fun `利用回数無制限のクーポンを作成する`() {
            // Arrange
            doNothing().whenever(couponRepository).persist(any<CouponEntity>())

            // Act
            val result =
                couponService.create(
                    code = "UNLIMITED",
                    discountId = discountId,
                    maxUses = null,
                    validFrom = null,
                    validUntil = null,
                )

            // Assert
            assertEquals("UNLIMITED", result.code)
            assertNull(result.maxUses)
            assertNull(result.validFrom)
            assertNull(result.validUntil)
            verify(couponRepository).persist(any<CouponEntity>())
        }
    }

    // === findByCode ===

    @Nested
    inner class FindByCode {
        @Test
        fun `コードでクーポンを取得する`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "SUMMER2026"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 5
                }
            whenever(couponRepository.findByCode("SUMMER2026")).thenReturn(entity)

            // Act
            val result = couponService.findByCode("SUMMER2026")

            // Assert
            assertNotNull(result)
            assertEquals("SUMMER2026", result!!.code)
            verify(tenantFilterService).enableFilter()
            verify(couponRepository).findByCode("SUMMER2026")
        }

        @Test
        fun `存在しないコードの場合はnullを返す`() {
            // Arrange
            whenever(couponRepository.findByCode("INVALID")).thenReturn(null)

            // Act
            val result = couponService.findByCode("INVALID")

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `IDでクーポンを取得する`() {
            // Arrange
            val couponId = UUID.randomUUID()
            val entity =
                CouponEntity().apply {
                    this.id = couponId
                    this.organizationId = orgId
                    this.code = "TESTCODE"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 0
                }
            whenever(couponRepository.findById(couponId)).thenReturn(entity)

            // Act
            val result = couponService.findById(couponId)

            // Assert
            assertNotNull(result)
            assertEquals(couponId, result!!.id)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val couponId = UUID.randomUUID()
            whenever(couponRepository.findById(couponId)).thenReturn(null)

            // Act
            val result = couponService.findById(couponId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === validate ===

    @Nested
    inner class Validate {
        @Test
        fun `存在しないクーポンコードはNOT_FOUNDを返す`() {
            // Arrange
            whenever(couponRepository.findByCode("NOTEXIST")).thenReturn(null)

            // Act
            val result = couponService.validate("NOTEXIST")

            // Assert
            assertFalse(result.isValid)
            assertNull(result.coupon)
            assertEquals("NOT_FOUND", result.reason)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `有効期間開始前のクーポンはNOT_YET_VALIDを返す`() {
            // Arrange
            val futureStart = Instant.now().plus(7, ChronoUnit.DAYS)
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "FUTURE"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 0
                    this.validFrom = futureStart
                    this.validUntil = futureStart.plus(30, ChronoUnit.DAYS)
                }
            whenever(couponRepository.findByCode("FUTURE")).thenReturn(entity)

            // Act
            val result = couponService.validate("FUTURE")

            // Assert
            assertFalse(result.isValid)
            assertNotNull(result.coupon)
            assertEquals("NOT_YET_VALID", result.reason)
        }

        @Test
        fun `有効期間終了後のクーポンはEXPIREDを返す`() {
            // Arrange
            val pastEnd = Instant.now().minus(1, ChronoUnit.DAYS)
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "EXPIRED"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 0
                    this.validFrom = pastEnd.minus(30, ChronoUnit.DAYS)
                    this.validUntil = pastEnd
                }
            whenever(couponRepository.findByCode("EXPIRED")).thenReturn(entity)

            // Act
            val result = couponService.validate("EXPIRED")

            // Assert
            assertFalse(result.isValid)
            assertNotNull(result.coupon)
            assertEquals("EXPIRED", result.reason)
        }

        @Test
        fun `利用回数上限に達したクーポンはMAX_USES_REACHEDを返す`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "MAXED"
                    this.discountId = this@CouponServiceTest.discountId
                    this.maxUses = 10
                    this.usedCount = 10
                    this.validFrom = Instant.now().minus(7, ChronoUnit.DAYS)
                    this.validUntil = Instant.now().plus(7, ChronoUnit.DAYS)
                }
            whenever(couponRepository.findByCode("MAXED")).thenReturn(entity)

            // Act
            val result = couponService.validate("MAXED")

            // Assert
            assertFalse(result.isValid)
            assertNotNull(result.coupon)
            assertEquals("MAX_USES_REACHED", result.reason)
        }

        @Test
        fun `紐付き割引が無効の場合はDISCOUNT_INACTIVEを返す`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "INACTIVE_DISC"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 0
                    this.validFrom = Instant.now().minus(7, ChronoUnit.DAYS)
                    this.validUntil = Instant.now().plus(7, ChronoUnit.DAYS)
                }
            val inactiveDiscount =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "無効割引"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.isActive = false
                }
            whenever(couponRepository.findByCode("INACTIVE_DISC")).thenReturn(entity)
            whenever(discountRepository.findById(discountId)).thenReturn(inactiveDiscount)

            // Act
            val result = couponService.validate("INACTIVE_DISC")

            // Assert
            assertFalse(result.isValid)
            assertNotNull(result.coupon)
            assertEquals("DISCOUNT_INACTIVE", result.reason)
        }

        @Test
        fun `紐付き割引が存在しない場合はDISCOUNT_INACTIVEを返す`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "NO_DISC"
                    this.discountId = this@CouponServiceTest.discountId
                    this.usedCount = 0
                    this.validFrom = Instant.now().minus(7, ChronoUnit.DAYS)
                    this.validUntil = Instant.now().plus(7, ChronoUnit.DAYS)
                }
            whenever(couponRepository.findByCode("NO_DISC")).thenReturn(entity)
            whenever(discountRepository.findById(discountId)).thenReturn(null)

            // Act
            val result = couponService.validate("NO_DISC")

            // Assert
            assertFalse(result.isValid)
            assertNotNull(result.coupon)
            assertEquals("DISCOUNT_INACTIVE", result.reason)
        }

        @Test
        fun `全条件を満たすクーポンは有効と判定される`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "VALID"
                    this.discountId = this@CouponServiceTest.discountId
                    this.maxUses = 100
                    this.usedCount = 5
                    this.validFrom = Instant.now().minus(7, ChronoUnit.DAYS)
                    this.validUntil = Instant.now().plus(7, ChronoUnit.DAYS)
                }
            val activeDiscount =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "有効割引"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.isActive = true
                }
            whenever(couponRepository.findByCode("VALID")).thenReturn(entity)
            whenever(discountRepository.findById(discountId)).thenReturn(activeDiscount)

            // Act
            val result = couponService.validate("VALID")

            // Assert
            assertTrue(result.isValid)
            assertNotNull(result.coupon)
            assertNull(result.reason)
            assertEquals("VALID", result.coupon!!.code)
        }

        @Test
        fun `利用回数無制限かつ期間無制限のクーポンは有効と判定される`() {
            // Arrange
            val entity =
                CouponEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "UNLIMITED"
                    this.discountId = this@CouponServiceTest.discountId
                    this.maxUses = null
                    this.usedCount = 999
                    this.validFrom = null
                    this.validUntil = null
                }
            val activeDiscount =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "常時有効割引"
                    this.discountType = "FIXED_AMOUNT"
                    this.value = 50000
                    this.isActive = true
                }
            whenever(couponRepository.findByCode("UNLIMITED")).thenReturn(entity)
            whenever(discountRepository.findById(discountId)).thenReturn(activeDiscount)

            // Act
            val result = couponService.validate("UNLIMITED")

            // Assert
            assertTrue(result.isValid)
            assertNotNull(result.coupon)
            assertNull(result.reason)
        }
    }
}
