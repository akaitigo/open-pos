package com.openpos.product.service

import com.openpos.product.cache.ProductCacheService
import com.openpos.product.config.OrganizationIdHolder
import com.openpos.product.config.TenantFilterService
import com.openpos.product.entity.DiscountEntity
import com.openpos.product.repository.DiscountRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class DiscountServiceTest {
    private lateinit var discountService: DiscountService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private lateinit var discountRepository: DiscountRepository

    private lateinit var tenantFilterService: TenantFilterService

    private lateinit var cacheService: ProductCacheService

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        discountRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()
        cacheService = mock()

        discountService = DiscountService()
        discountService.discountRepository = discountRepository
        discountService.tenantFilterService = tenantFilterService
        discountService.organizationIdHolder = organizationIdHolder
        discountService.cacheService = cacheService
        discountService.objectMapper =
            com.fasterxml.jackson.databind
                .ObjectMapper()

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    // === create ===

    @Nested
    inner class Create {
        @Test
        fun `パーセント割引を作成する`() {
            // Arrange
            val validFrom = Instant.now()
            val validUntil = Instant.now().plus(30, ChronoUnit.DAYS)
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.create(
                    name = "夏セール10%OFF",
                    discountType = "PERCENTAGE",
                    value = 10,
                    validFrom = validFrom,
                    validUntil = validUntil,
                )

            // Assert
            assertEquals("夏セール10%OFF", result.name)
            assertEquals("PERCENTAGE", result.discountType)
            assertEquals(10L, result.value)
            assertEquals(validFrom, result.validFrom)
            assertEquals(validUntil, result.validUntil)
            assertTrue(result.isActive)
            assertEquals(orgId, result.organizationId)
            verify(discountRepository).persist(any<DiscountEntity>())
        }

        @Test
        fun `定額割引を作成する`() {
            // Arrange
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.create(
                    name = "500円引き",
                    discountType = "FIXED_AMOUNT",
                    value = 50000L,
                    validFrom = null,
                    validUntil = null,
                )

            // Assert
            assertEquals("500円引き", result.name)
            assertEquals("FIXED_AMOUNT", result.discountType)
            assertEquals(50000L, result.value)
            assertNull(result.validFrom)
            assertNull(result.validUntil)
            assertTrue(result.isActive)
            verify(discountRepository).persist(any<DiscountEntity>())
        }

        @Test
        fun `パーセント割引の値が0の場合は作成できる`() {
            // Arrange
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.create(
                    name = "0%割引",
                    discountType = "PERCENTAGE",
                    value = 0,
                    validFrom = null,
                    validUntil = null,
                )

            // Assert
            assertEquals(0L, result.value)
        }

        @Test
        fun `パーセント割引の値が100の場合は作成できる`() {
            // Arrange
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.create(
                    name = "100%割引",
                    discountType = "PERCENTAGE",
                    value = 100,
                    validFrom = null,
                    validUntil = null,
                )

            // Assert
            assertEquals(100L, result.value)
        }

        @Test
        fun `パーセント割引の値が101以上の場合はIllegalArgumentExceptionを投げる`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                discountService.create(
                    name = "不正割引",
                    discountType = "PERCENTAGE",
                    value = 101,
                    validFrom = null,
                    validUntil = null,
                )
            }
        }

        @Test
        fun `パーセント割引の値が負数の場合はIllegalArgumentExceptionを投げる`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                discountService.create(
                    name = "不正割引",
                    discountType = "PERCENTAGE",
                    value = -1,
                    validFrom = null,
                    validUntil = null,
                )
            }
        }

        @Test
        fun `定額割引の値が負数の場合はIllegalArgumentExceptionを投げる`() {
            // Act & Assert
            assertThrows(IllegalArgumentException::class.java) {
                discountService.create(
                    name = "不正割引",
                    discountType = "FIXED_AMOUNT",
                    value = -1,
                    validFrom = null,
                    validUntil = null,
                )
            }
        }

        @Test
        fun `定額割引の値が0の場合は作成できる`() {
            // Arrange
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.create(
                    name = "0円割引",
                    discountType = "FIXED_AMOUNT",
                    value = 0,
                    validFrom = null,
                    validUntil = null,
                )

            // Assert
            assertEquals(0L, result.value)
        }
    }

    // === list ===

    @Nested
    inner class List {
        @Test
        fun `activeOnly=trueの場合は有効かつ期間内の割引のみ返す`() {
            // Arrange
            val discount1 =
                DiscountEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "有効割引A"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.isActive = true
                }
            whenever(discountRepository.findActiveAndValid(any())).thenReturn(listOf(discount1))

            // Act
            val result = discountService.list(activeOnly = true)

            // Assert
            assertEquals(1, result.size)
            assertEquals("有効割引A", result[0].name)
            verify(tenantFilterService).enableFilter()
            verify(discountRepository).findActiveAndValid(any())
        }

        @Test
        fun `activeOnly=falseの場合は全割引を返す`() {
            // Arrange
            val discount1 =
                DiscountEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "有効割引"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.isActive = true
                }
            val discount2 =
                DiscountEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.name = "無効割引"
                    this.discountType = "FIXED_AMOUNT"
                    this.value = 50000
                    this.isActive = false
                }
            whenever(discountRepository.findAllOrdered()).thenReturn(listOf(discount1, discount2))

            // Act
            val result = discountService.list(activeOnly = false)

            // Assert
            assertEquals(2, result.size)
            assertEquals("有効割引", result[0].name)
            assertEquals("無効割引", result[1].name)
            verify(tenantFilterService).enableFilter()
            verify(discountRepository).findAllOrdered()
        }

        @Test
        fun `割引が存在しない場合は空リストを返す`() {
            // Arrange
            whenever(discountRepository.findActiveAndValid(any())).thenReturn(emptyList())

            // Act
            val result = discountService.list(activeOnly = true)

            // Assert
            assertEquals(0, result.size)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === findById ===

    @Nested
    inner class FindById {
        @Test
        fun `IDで割引を取得する`() {
            // Arrange
            val discountId = UUID.randomUUID()
            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "テスト割引"
                    this.discountType = "PERCENTAGE"
                    this.value = 15
                    this.isActive = true
                }
            val mockQuery1 = mock<PanacheQuery<DiscountEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(discountRepository.find(eq("id = ?1"), eq(discountId))).thenReturn(mockQuery1)

            // Act
            val result = discountService.findById(discountId)

            // Assert
            assertNotNull(result)
            assertEquals(discountId, result!!.id)
            assertEquals("テスト割引", result.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `存在しないIDの場合はnullを返す`() {
            // Arrange
            val discountId = UUID.randomUUID()
            val mockQuery2 = mock<PanacheQuery<DiscountEntity>>()
            whenever(mockQuery2.firstResult()).thenReturn(null)
            whenever(discountRepository.find(eq("id = ?1"), eq(discountId))).thenReturn(mockQuery2)

            // Act
            val result = discountService.findById(discountId)

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }

    // === update ===

    @Nested
    inner class Update {
        @Test
        fun `割引の値を更新する`() {
            // Arrange
            val discountId = UUID.randomUUID()
            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "旧割引"
                    this.discountType = "PERCENTAGE"
                    this.value = 10
                    this.validFrom = Instant.now().minus(7, ChronoUnit.DAYS)
                    this.validUntil = Instant.now().plus(7, ChronoUnit.DAYS)
                    this.isActive = true
                }
            val mockQuery3 = mock<PanacheQuery<DiscountEntity>>()
            whenever(mockQuery3.firstResult()).thenReturn(entity)
            whenever(discountRepository.find(eq("id = ?1"), eq(discountId))).thenReturn(mockQuery3)
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.update(
                    id = discountId,
                    name = null,
                    discountType = null,
                    value = 20L,
                    validFrom = null,
                    validUntil = null,
                    isActive = null,
                )

            // Assert
            assertNotNull(result)
            assertEquals("旧割引", result!!.name) // name は更新されない
            assertEquals("PERCENTAGE", result.discountType) // discountType は更新されない
            assertEquals(20L, result.value)
            assertTrue(result.isActive)
            verify(tenantFilterService).enableFilter()
            verify(discountRepository).persist(any<DiscountEntity>())
        }

        @Test
        fun `割引を無効化する`() {
            // Arrange
            val discountId = UUID.randomUUID()
            val entity =
                DiscountEntity().apply {
                    this.id = discountId
                    this.organizationId = orgId
                    this.name = "無効化対象"
                    this.discountType = "FIXED_AMOUNT"
                    this.value = 50000
                    this.isActive = true
                }
            val mockQuery4 = mock<PanacheQuery<DiscountEntity>>()
            whenever(mockQuery4.firstResult()).thenReturn(entity)
            whenever(discountRepository.find(eq("id = ?1"), eq(discountId))).thenReturn(mockQuery4)
            doNothing().whenever(discountRepository).persist(any<DiscountEntity>())

            // Act
            val result =
                discountService.update(
                    id = discountId,
                    name = null,
                    discountType = null,
                    value = null,
                    validFrom = null,
                    validUntil = null,
                    isActive = false,
                )

            // Assert
            assertNotNull(result)
            assertEquals(false, result!!.isActive)
            verify(discountRepository).persist(any<DiscountEntity>())
        }

        @Test
        fun `存在しない割引の更新はnullを返す`() {
            // Arrange
            val discountId = UUID.randomUUID()
            val mockQuery5 = mock<PanacheQuery<DiscountEntity>>()
            whenever(mockQuery5.firstResult()).thenReturn(null)
            whenever(discountRepository.find(eq("id = ?1"), eq(discountId))).thenReturn(mockQuery5)

            // Act
            val result =
                discountService.update(
                    id = discountId,
                    name = "新名前",
                    discountType = null,
                    value = null,
                    validFrom = null,
                    validUntil = null,
                    isActive = null,
                )

            // Assert
            assertNull(result)
            verify(tenantFilterService).enableFilter()
        }
    }
}
