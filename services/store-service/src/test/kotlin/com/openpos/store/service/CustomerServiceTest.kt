package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.CustomerEntity
import com.openpos.store.entity.PointTransactionEntity
import com.openpos.store.repository.CustomerRepository
import com.openpos.store.repository.PointTransactionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class CustomerServiceTest {
    private lateinit var service: CustomerService
    private val customerRepository = mock<CustomerRepository>()
    private val pointTransactionRepository = mock<PointTransactionRepository>()
    private val tenantFilterService = mock<TenantFilterService>()
    private val organizationIdHolder = mock<OrganizationIdHolder>()

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        service =
            CustomerService().apply {
                this.customerRepository = this@CustomerServiceTest.customerRepository
                this.pointTransactionRepository = this@CustomerServiceTest.pointTransactionRepository
                this.tenantFilterService = this@CustomerServiceTest.tenantFilterService
                this.organizationIdHolder = this@CustomerServiceTest.organizationIdHolder
            }
        whenever(organizationIdHolder.organizationId).thenReturn(orgId)
    }

    @Test
    fun `create sets organizationId and default points to 0`() {
        // Arrange & Act
        val result = service.create("テスト顧客", "test@example.com", "090-1234-5678")

        // Assert
        assertNotNull(result)
        assertEquals("テスト顧客", result.name)
        assertEquals(orgId, result.organizationId)
        assertEquals(0, result.points)
        verify(customerRepository).persist(any<CustomerEntity>())
    }

    @Test
    fun `earnPoints adds points based on transaction total (1 point per 100 yen)`() {
        // Arrange
        val customerId = UUID.randomUUID()
        val customer =
            CustomerEntity().apply {
                id = customerId
                organizationId = orgId
                name = "テスト顧客"
                points = 100
            }
        whenever(customerRepository.findById(customerId)).thenReturn(customer)

        // Act: 50000 sen = 500 yen => 5 points
        val earned = service.earnPoints(customerId, 50000, null)

        // Assert
        assertEquals(5L, earned)
        assertEquals(105, customer.points)
        verify(pointTransactionRepository).persist(any<PointTransactionEntity>())
    }

    @Test
    fun `earnPoints returns 0 when transaction total is less than 100 yen`() {
        // Arrange
        val customerId = UUID.randomUUID()
        val customer =
            CustomerEntity().apply {
                id = customerId
                organizationId = orgId
                name = "テスト顧客"
                points = 100
            }
        whenever(customerRepository.findById(customerId)).thenReturn(customer)

        // Act: 9999 sen = 99.99 yen => 0 points
        val earned = service.earnPoints(customerId, 9999, null)

        // Assert
        assertEquals(0L, earned)
        assertEquals(100, customer.points) // unchanged
    }

    @Test
    fun `redeemPoints fails when insufficient points`() {
        // Arrange
        val customerId = UUID.randomUUID()
        val customer =
            CustomerEntity().apply {
                id = customerId
                organizationId = orgId
                name = "テスト顧客"
                points = 30
            }
        whenever(customerRepository.findById(customerId)).thenReturn(customer)

        // Act
        val result = service.redeemPoints(customerId, 50, null)

        // Assert
        assertFalse(result)
    }

    @Test
    fun `redeemPoints succeeds when sufficient points`() {
        // Arrange
        val customerId = UUID.randomUUID()
        val customer =
            CustomerEntity().apply {
                id = customerId
                organizationId = orgId
                name = "テスト顧客"
                points = 100
            }
        whenever(customerRepository.findById(customerId)).thenReturn(customer)

        // Act
        val result = service.redeemPoints(customerId, 50, null)

        // Assert
        assertTrue(result)
        assertEquals(50, customer.points)
        verify(pointTransactionRepository).persist(any<PointTransactionEntity>())
    }
}
