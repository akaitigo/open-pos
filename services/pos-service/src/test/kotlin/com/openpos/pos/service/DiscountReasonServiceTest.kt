package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.repository.DiscountReasonRepository
import io.quarkus.hibernate.orm.panache.kotlin.PanacheQuery
import org.mockito.kotlin.mock
import org.mockito.kotlin.eq
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.util.UUID

class DiscountReasonServiceTest {
    private lateinit var discountReasonService: DiscountReasonService

    private lateinit var discountReasonRepository: DiscountReasonRepository

    private lateinit var tenantFilterService: TenantFilterService

    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        discountReasonRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        discountReasonService = DiscountReasonService()
        discountReasonService.discountReasonRepository = discountReasonRepository
        discountReasonService.tenantFilterService = tenantFilterService
        discountReasonService.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = testOrgId
        doNothing().whenever(discountReasonRepository).persist(any<DiscountReasonEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
    }

    @Test
    fun `create should create discount reason with correct fields`() {
        val reason =
            discountReasonService.create(
                code = "DAMAGE",
                description = "商品損傷",
            )

        assertNotNull(reason)
        assertEquals("DAMAGE", reason.code)
        assertEquals("商品損傷", reason.description)
        assertEquals(true, reason.isActive)
    }

    @Test
    fun `update should modify fields`() {
        val reasonId = UUID.randomUUID()
        val entity =
            DiscountReasonEntity().apply {
                id = reasonId
                organizationId = testOrgId
                code = "DAMAGE"
                description = "商品損傷"
                isActive = true
            }
        val mockQuery1 = mock<PanacheQuery<DiscountReasonEntity>>()
            whenever(mockQuery1.firstResult()).thenReturn(entity)
            whenever(discountReasonRepository.find(eq("id = ?1"), eq(reasonId))).thenReturn(mockQuery1)

        val result =
            discountReasonService.update(
                id = reasonId,
                description = "商品破損",
                isActive = false,
            )

        assertNotNull(result)
        assertEquals("商品破損", result?.description)
        assertEquals(false, result?.isActive)
    }

    @Test
    fun `listActive should return active reasons`() {
        val reason =
            DiscountReasonEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                code = "PROMO"
                description = "キャンペーン"
                isActive = true
            }
        whenever(discountReasonRepository.findActive()).thenReturn(listOf(reason))

        val result = discountReasonService.listActive()

        assertEquals(1, result.size)
        assertEquals("PROMO", result[0].code)
    }

    @Test
    fun `listAll should return all reasons including inactive`() {
        val active =
            DiscountReasonEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                code = "PROMO"
                description = "キャンペーン"
                isActive = true
            }
        val inactive =
            DiscountReasonEntity().apply {
                id = UUID.randomUUID()
                organizationId = testOrgId
                code = "OLD_PROMO"
                description = "旧キャンペーン"
                isActive = false
            }
        whenever(discountReasonRepository.findAllOrdered()).thenReturn(listOf(active, inactive))

        val result = discountReasonService.listAll()

        assertEquals(2, result.size)
        assertEquals("PROMO", result[0].code)
        assertEquals(true, result[0].isActive)
        assertEquals("OLD_PROMO", result[1].code)
        assertEquals(false, result[1].isActive)
    }
}
