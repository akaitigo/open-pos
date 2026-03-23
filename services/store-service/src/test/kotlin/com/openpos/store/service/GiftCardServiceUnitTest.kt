package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.GiftCardEntity
import com.openpos.store.repository.GiftCardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class GiftCardServiceUnitTest {
    private lateinit var service: GiftCardService
    private lateinit var giftCardRepository: GiftCardRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        giftCardRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = GiftCardService()
        service.giftCardRepository = giftCardRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(giftCardRepository).persist(any<GiftCardEntity>())
    }

    @Nested
    inner class Activate {
        @Test
        fun `creates gift card with ACTIVE status`() {
            val result = service.activate("GIFT-001", 500000)

            assertEquals("GIFT-001", result.code)
            assertEquals(500000L, result.balance)
            assertEquals(500000L, result.initialBalance)
            assertEquals("ACTIVE", result.status)
            assertEquals(orgId, result.organizationId)
            verify(giftCardRepository).persist(any<GiftCardEntity>())
        }
    }

    @Nested
    inner class FindByCode {
        @Test
        fun `returns card when found`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 10000
                    initialBalance = 10000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findByCode("GIFT-001")).thenReturn(card)

            val result = service.findByCode("GIFT-001")

            assertNotNull(result)
            assertEquals("GIFT-001", result?.code)
        }

        @Test
        fun `returns null when not found`() {
            whenever(giftCardRepository.findByCode("NONE")).thenReturn(null)

            assertNull(service.findByCode("NONE"))
        }
    }

    @Nested
    inner class FindById {
        @Test
        fun `returns card when found`() {
            val cardId = UUID.randomUUID()
            val card =
                GiftCardEntity().apply {
                    id = cardId
                    organizationId = orgId
                    code = "GIFT-002"
                    balance = 20000
                    initialBalance = 20000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findById(cardId)).thenReturn(card)

            val result = service.findById(cardId)

            assertNotNull(result)
        }
    }

    @Nested
    inner class Charge {
        @Test
        fun `adds balance to active card`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 10000
                    initialBalance = 10000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findByCode("GIFT-001")).thenReturn(card)

            val result = service.charge("GIFT-001", 50000)

            assertNotNull(result)
            assertEquals(60000L, result?.balance)
        }

        @Test
        fun `returns null for non-ACTIVE card`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 10000
                    initialBalance = 10000
                    status = "USED"
                }
            whenever(giftCardRepository.findByCode("GIFT-001")).thenReturn(card)

            val result = service.charge("GIFT-001", 50000)

            assertNull(result)
        }

        @Test
        fun `returns null when card not found`() {
            whenever(giftCardRepository.findByCode("NONE")).thenReturn(null)

            assertNull(service.charge("NONE", 50000))
        }
    }

    @Nested
    inner class Redeem {
        @Test
        fun `deducts balance and sets USED when zero`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 10000
                    initialBalance = 10000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findByCodeForUpdate("GIFT-001")).thenReturn(card)

            val result = service.redeem("GIFT-001", 10000)

            assertNotNull(result)
            assertEquals(0L, result?.balance)
            assertEquals("USED", result?.status)
        }

        @Test
        fun `deducts balance partially`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 50000
                    initialBalance = 50000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findByCodeForUpdate("GIFT-001")).thenReturn(card)

            val result = service.redeem("GIFT-001", 20000)

            assertNotNull(result)
            assertEquals(30000L, result?.balance)
            assertEquals("ACTIVE", result?.status)
        }

        @Test
        fun `returns null for insufficient balance`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 1000
                    initialBalance = 10000
                    status = "ACTIVE"
                }
            whenever(giftCardRepository.findByCodeForUpdate("GIFT-001")).thenReturn(card)

            assertNull(service.redeem("GIFT-001", 5000))
        }

        @Test
        fun `returns null for non-ACTIVE card`() {
            val card =
                GiftCardEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    code = "GIFT-001"
                    balance = 10000
                    initialBalance = 10000
                    status = "USED"
                }
            whenever(giftCardRepository.findByCodeForUpdate("GIFT-001")).thenReturn(card)

            assertNull(service.redeem("GIFT-001", 5000))
        }

        @Test
        fun `returns null when card not found`() {
            whenever(giftCardRepository.findByCodeForUpdate("NONE")).thenReturn(null)

            assertNull(service.redeem("NONE", 5000))
        }
    }
}
