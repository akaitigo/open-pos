package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.StampCardEntity
import com.openpos.store.repository.StampCardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.Instant
import java.util.UUID

class StampCardServiceUnitTest {
    private val stampCardRepository: StampCardRepository = mock()
    private val tenantFilterService: TenantFilterService = mock()
    private val organizationIdHolder = OrganizationIdHolder()

    private val service =
        StampCardService().also {
            setField(it, "stampCardRepository", stampCardRepository)
            setField(it, "tenantFilterService", tenantFilterService)
            setField(it, "organizationIdHolder", organizationIdHolder)
        }

    private val orgId = UUID.randomUUID()
    private val customerId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
    }

    @Nested
    inner class Issue {
        @Test
        fun `正常にスタンプカードを発行する`() {
            val card = service.issue(customerId, 10, "ドリンク1杯無料")
            verify(stampCardRepository).persist(any<StampCardEntity>())
            assertEquals(0, card.stampCount)
            assertEquals(10, card.maxStamps)
            assertEquals("ドリンク1杯無料", card.rewardDescription)
            assertEquals("ACTIVE", card.status)
        }

        @Test
        fun `maxStamps 0以下はデフォルト10になる`() {
            val card = service.issue(customerId, 0, null)
            assertEquals(10, card.maxStamps)
        }
    }

    @Nested
    inner class AddStamp {
        @Test
        fun `スタンプを追加する`() {
            val entity = buildCard(stampCount = 3, maxStamps = 10)
            whenever(stampCardRepository.findActiveByCustomerId(customerId)).thenReturn(entity)
            val result = service.addStamp(customerId)
            assertEquals(4, result.stampCount)
            assertEquals("ACTIVE", result.status)
        }

        @Test
        fun `最大到達でCOMPLETEDになる`() {
            val entity = buildCard(stampCount = 9, maxStamps = 10)
            whenever(stampCardRepository.findActiveByCustomerId(customerId)).thenReturn(entity)
            val result = service.addStamp(customerId)
            assertEquals(10, result.stampCount)
            assertEquals("COMPLETED", result.status)
        }

        @Test
        fun `アクティブカードがなければエラー`() {
            whenever(stampCardRepository.findActiveByCustomerId(customerId)).thenReturn(null)
            assertThrows(IllegalArgumentException::class.java) {
                service.addStamp(customerId)
            }
        }
    }

    @Nested
    inner class RedeemReward {
        @Test
        fun `報酬交換でリセットされる`() {
            val entity = buildCard(stampCount = 10, maxStamps = 10, status = "COMPLETED")
            whenever(stampCardRepository.findByCustomerId(customerId)).thenReturn(entity)
            val result = service.redeemReward(customerId)
            assertEquals(0, result.stampCount)
            assertEquals("ACTIVE", result.status)
        }

        @Test
        fun `COMPLETED以外では交換できない`() {
            val entity = buildCard(stampCount = 5, maxStamps = 10)
            whenever(stampCardRepository.findByCustomerId(customerId)).thenReturn(entity)
            assertThrows(IllegalArgumentException::class.java) {
                service.redeemReward(customerId)
            }
        }
    }

    private fun buildCard(
        stampCount: Int = 0,
        maxStamps: Int = 10,
        status: String = "ACTIVE",
    ): StampCardEntity =
        StampCardEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.customerId = this@StampCardServiceUnitTest.customerId
            this.stampCount = stampCount
            this.maxStamps = maxStamps
            this.status = status
            this.issuedAt = Instant.now()
        }

    companion object {
        fun setField(
            target: Any,
            fieldName: String,
            value: Any,
        ) {
            val field = target::class.java.getDeclaredField(fieldName)
            field.isAccessible = true
            field.set(target, value)
        }
    }
}
