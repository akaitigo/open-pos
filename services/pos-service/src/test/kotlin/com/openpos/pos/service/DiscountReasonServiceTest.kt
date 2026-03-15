package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.DiscountReasonEntity
import com.openpos.pos.repository.DiscountReasonRepository
import io.quarkus.test.InjectMock
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.whenever
import java.util.UUID

@QuarkusTest
class DiscountReasonServiceTest {
    @Inject
    lateinit var discountReasonService: DiscountReasonService

    @InjectMock
    lateinit var discountReasonRepository: DiscountReasonRepository

    @InjectMock
    lateinit var tenantFilterService: TenantFilterService

    @InjectMock
    lateinit var organizationIdHolder: OrganizationIdHolder

    private val testOrgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        doNothing().whenever(discountReasonRepository).persist(any<DiscountReasonEntity>())
        doNothing().whenever(tenantFilterService).enableFilter()
        whenever(organizationIdHolder.organizationId).thenReturn(testOrgId)
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
        whenever(discountReasonRepository.findById(reasonId)).thenReturn(entity)

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
}
