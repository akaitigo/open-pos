package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.repository.DiscountReasonRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

/**
 * DiscountReasonService の純粋ユニットテスト。
 * CDI プロキシを回避してカバレッジを確保する。
 */
class DiscountReasonServiceUnitTest {
    private lateinit var service: DiscountReasonService
    private lateinit var discountReasonRepository: DiscountReasonRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        discountReasonRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = DiscountReasonService()
        service.discountReasonRepository = discountReasonRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Nested
    inner class Create {
        @Test
        fun `creates discount reason with correct fields`() {
            doNothing().whenever(discountReasonRepository).persist(any<DiscountReasonEntity>())

            val result = service.create("DAMAGE", "商品損傷")

            assertEquals("DAMAGE", result.code)
            assertEquals("商品損傷", result.description)
            assertEquals(orgId, result.organizationId)
        }

        @Test
        fun `throws when organizationId is not set`() {
            organizationIdHolder.organizationId = null

            assertThrows(IllegalArgumentException::class.java) {
                service.create("DAMAGE", "商品損傷")
            }
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `returns null when not found`() {
            whenever(discountReasonRepository.findById(any<UUID>())).thenReturn(null)

            val result = service.update(UUID.randomUUID(), "新しい説明", null)

            assertNull(result)
        }

        @Test
        fun `updates description only`() {
            val id = UUID.randomUUID()
            val entity =
                DiscountReasonEntity().apply {
                    this.id = id
                    this.organizationId = orgId
                    this.code = "DAMAGE"
                    this.description = "旧説明"
                    this.isActive = true
                }
            whenever(discountReasonRepository.findById(id)).thenReturn(entity)
            doNothing().whenever(discountReasonRepository).persist(any<DiscountReasonEntity>())

            val result = service.update(id, "新しい説明", null)

            assertNotNull(result)
            assertEquals("新しい説明", result?.description)
            assertEquals(true, result?.isActive)
        }
    }

    @Nested
    inner class ListActive {
        @Test
        fun `returns active reasons`() {
            val entity =
                DiscountReasonEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "PROMO"
                    this.description = "キャンペーン"
                    this.isActive = true
                }
            whenever(discountReasonRepository.findActive()).thenReturn(listOf(entity))

            val result = service.listActive()

            assertEquals(1, result.size)
            assertEquals("PROMO", result[0].code)
        }
    }

    @Nested
    inner class ListAll {
        @Test
        fun `returns all reasons including inactive`() {
            val active =
                DiscountReasonEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "PROMO"
                    this.description = "キャンペーン"
                    this.isActive = true
                }
            val inactive =
                DiscountReasonEntity().apply {
                    this.id = UUID.randomUUID()
                    this.organizationId = orgId
                    this.code = "OLD_PROMO"
                    this.description = "旧キャンペーン"
                    this.isActive = false
                }
            whenever(discountReasonRepository.findAllOrdered()).thenReturn(listOf(active, inactive))

            val result = service.listAll()

            assertEquals(2, result.size)
            assertEquals("PROMO", result[0].code)
            assertEquals(true, result[0].isActive)
            assertEquals("OLD_PROMO", result[1].code)
            assertEquals(false, result[1].isActive)
        }
    }
}
