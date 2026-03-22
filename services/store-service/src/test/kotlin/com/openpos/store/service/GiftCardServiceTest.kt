package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.GiftCardEntity
import com.openpos.store.repository.GiftCardRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class GiftCardServiceTest {
    private lateinit var service: GiftCardService
    private val giftCardRepository = mock<GiftCardRepository>()
    private val tenantFilterService = mock<TenantFilterService>()
    private val organizationIdHolder = mock<OrganizationIdHolder>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            GiftCardService().apply {
                this.giftCardRepository = this@GiftCardServiceTest.giftCardRepository
                this.tenantFilterService = this@GiftCardServiceTest.tenantFilterService
                this.organizationIdHolder = this@GiftCardServiceTest.organizationIdHolder
            }
        whenever(organizationIdHolder.organizationId).thenReturn(orgId)
    }

    @Test
    fun `activate sets initial balance and ACTIVE status`() {
        // Arrange & Act
        val result = service.activate("GIFT-001", 500000) // 5000 yen

        // Assert
        assertEquals("GIFT-001", result.code)
        assertEquals(500000L, result.initialBalance)
        assertEquals(500000L, result.balance)
        assertEquals("ACTIVE", result.status)
        verify(giftCardRepository).persist(any<GiftCardEntity>())
    }

    @Test
    fun `redeem returns null when balance insufficient`() {
        // Arrange
        val card =
            GiftCardEntity().apply {
                id = UUID.randomUUID()
                organizationId = orgId
                code = "GIFT-001"
                balance = 10000 // 100 yen
                initialBalance = 500000
                status = "ACTIVE"
            }
        whenever(giftCardRepository.findByCode("GIFT-001")).thenReturn(card)

        // Act
        val result = service.redeem("GIFT-001", 20000) // 200 yen > 100 yen

        // Assert
        assertNull(result)
    }

    @Test
    fun `redeem deducts balance and sets USED when zero`() {
        // Arrange
        val card =
            GiftCardEntity().apply {
                id = UUID.randomUUID()
                organizationId = orgId
                code = "GIFT-001"
                balance = 10000 // 100 yen
                initialBalance = 10000
                status = "ACTIVE"
            }
        whenever(giftCardRepository.findByCode("GIFT-001")).thenReturn(card)

        // Act
        val result = service.redeem("GIFT-001", 10000)

        // Assert
        assertNotNull(result)
        assertEquals(0L, result?.balance)
        assertEquals("USED", result?.status)
    }

    @Test
    fun `charge adds balance to active card`() {
        // Arrange
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

        // Act
        val result = service.charge("GIFT-001", 50000) // charge 500 yen

        // Assert
        assertNotNull(result)
        assertEquals(60000L, result?.balance)
    }

    @Test
    fun `redeem returns null for non-ACTIVE card`() {
        // Arrange
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

        // Act
        val result = service.redeem("GIFT-001", 5000)

        // Assert
        assertNull(result)
    }
}
