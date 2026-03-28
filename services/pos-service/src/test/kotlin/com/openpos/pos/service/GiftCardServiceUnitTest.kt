package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.GiftCardEntity
import com.openpos.pos.repository.GiftCardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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

class GiftCardServiceUnitTest {
    private val giftCardRepository: GiftCardRepository = mock()
    private val tenantFilterService: TenantFilterService = mock()
    private val organizationIdHolder = OrganizationIdHolder()

    private val service =
        GiftCardService().also {
            setField(it, "giftCardRepository", giftCardRepository)
            setField(it, "tenantFilterService", tenantFilterService)
            setField(it, "organizationIdHolder", organizationIdHolder)
        }

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        organizationIdHolder.organizationId = orgId
    }

    @Nested
    inner class Create {
        @Test
        fun `正常にギフトカードを発行する`() {
            val card = service.create(50000, null)
            verify(giftCardRepository).persist(any<GiftCardEntity>())
            assertEquals(50000L, card.initialAmount)
            assertEquals(50000L, card.balance)
            assertEquals("PENDING", card.status)
            assertNotNull(card.code)
            assertEquals(19, card.code.length) // XXXX-XXXX-XXXX-XXXX
        }

        @Test
        fun `金額0以下はエラー`() {
            assertThrows(IllegalArgumentException::class.java) {
                service.create(0, null)
            }
        }
    }

    @Nested
    inner class Activate {
        @Test
        fun `PENDINGカードを有効化する`() {
            val entity = buildCard("PENDING")
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            val result = service.activate("TEST-CODE")
            assertEquals("ACTIVE", result.status)
        }

        @Test
        fun `ACTIVE状態のカードは有効化できない`() {
            val entity = buildCard("ACTIVE")
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            assertThrows(IllegalArgumentException::class.java) {
                service.activate("TEST-CODE")
            }
        }
    }

    @Nested
    inner class Redeem {
        @Test
        fun `正常に残高を利用する`() {
            val entity = buildCard("ACTIVE", balance = 50000)
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            val result = service.redeem("TEST-CODE", 30000)
            assertEquals(20000L, result.balance)
            assertEquals("ACTIVE", result.status)
        }

        @Test
        fun `残高を全額利用するとDEPLETEDになる`() {
            val entity = buildCard("ACTIVE", balance = 50000)
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            val result = service.redeem("TEST-CODE", 50000)
            assertEquals(0L, result.balance)
            assertEquals("DEPLETED", result.status)
        }

        @Test
        fun `残高不足はエラー`() {
            val entity = buildCard("ACTIVE", balance = 10000)
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            assertThrows(IllegalArgumentException::class.java) {
                service.redeem("TEST-CODE", 50000)
            }
        }

        @Test
        fun `ACTIVE以外のカードは利用できない`() {
            val entity = buildCard("DEPLETED", balance = 0)
            whenever(giftCardRepository.findByCode("TEST-CODE")).thenReturn(entity)
            assertThrows(IllegalArgumentException::class.java) {
                service.redeem("TEST-CODE", 10000)
            }
        }
    }

    private fun buildCard(
        status: String,
        balance: Long = 50000,
    ): GiftCardEntity =
        GiftCardEntity().apply {
            this.id = UUID.randomUUID()
            this.organizationId = orgId
            this.code = "TEST-CODE"
            this.initialAmount = 50000
            this.balance = balance
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
