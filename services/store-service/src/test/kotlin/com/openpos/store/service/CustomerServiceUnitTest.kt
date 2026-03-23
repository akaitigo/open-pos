package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.CustomerEntity
import com.openpos.store.entity.PointTransactionEntity
import com.openpos.store.repository.CustomerRepository
import com.openpos.store.repository.PointTransactionRepository
import io.quarkus.panache.common.Page
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.UUID

class CustomerServiceUnitTest {
    private lateinit var service: CustomerService
    private lateinit var customerRepository: CustomerRepository
    private lateinit var pointTransactionRepository: PointTransactionRepository
    private lateinit var tenantFilterService: TenantFilterService
    private lateinit var organizationIdHolder: OrganizationIdHolder

    private val orgId = UUID.randomUUID()

    @BeforeEach
    fun setUp() {
        customerRepository = mock()
        pointTransactionRepository = mock()
        tenantFilterService = mock()
        organizationIdHolder = OrganizationIdHolder()

        service = CustomerService()
        service.customerRepository = customerRepository
        service.pointTransactionRepository = pointTransactionRepository
        service.tenantFilterService = tenantFilterService
        service.organizationIdHolder = organizationIdHolder

        organizationIdHolder.organizationId = orgId
        doNothing().whenever(tenantFilterService).enableFilter()
        doNothing().whenever(customerRepository).persist(any<CustomerEntity>())
        doNothing().whenever(pointTransactionRepository).persist(any<PointTransactionEntity>())
    }

    @Nested
    inner class FindById {
        @Test
        fun `returns customer when found`() {
            val customerId = UUID.randomUUID()
            val entity =
                CustomerEntity().apply {
                    id = customerId
                    organizationId = orgId
                    name = "Test"
                }
            whenever(customerRepository.findById(customerId)).thenReturn(entity)

            val result = service.findById(customerId)

            assertNotNull(result)
            assertEquals("Test", result?.name)
            verify(tenantFilterService).enableFilter()
        }

        @Test
        fun `returns null when not found`() {
            val customerId = UUID.randomUUID()
            whenever(customerRepository.findById(customerId)).thenReturn(null)

            val result = service.findById(customerId)

            assertNull(result)
        }
    }

    @Nested
    inner class List {
        @Test
        fun `returns paginated list with total count`() {
            val customers =
                listOf(
                    CustomerEntity().apply {
                        id = UUID.randomUUID()
                        organizationId = orgId
                        name = "Customer A"
                    },
                )
            whenever(customerRepository.listPaginated(any<Page>())).thenReturn(customers)
            whenever(customerRepository.count()).thenReturn(10L)

            val (result, total) = service.list(0, 20)

            assertEquals(1, result.size)
            assertEquals(10L, total)
        }
    }

    @Nested
    inner class Update {
        @Test
        fun `updates customer fields selectively`() {
            val customerId = UUID.randomUUID()
            val entity =
                CustomerEntity().apply {
                    id = customerId
                    organizationId = orgId
                    name = "Old Name"
                    email = "old@test.com"
                    phone = "090-0000-0000"
                }
            whenever(customerRepository.findById(customerId)).thenReturn(entity)

            val result = service.update(customerId, "New Name", null, "090-1111-1111")

            assertNotNull(result)
            assertEquals("New Name", result?.name)
            assertEquals("old@test.com", result?.email)
            assertEquals("090-1111-1111", result?.phone)
        }

        @Test
        fun `returns null when customer not found`() {
            val customerId = UUID.randomUUID()
            whenever(customerRepository.findById(customerId)).thenReturn(null)

            val result = service.update(customerId, "New", null, null)

            assertNull(result)
        }
    }

    @Nested
    inner class EarnPoints {
        @Test
        fun `throws when customer not found`() {
            val customerId = UUID.randomUUID()
            whenever(customerRepository.findById(customerId)).thenReturn(null)

            assertThrows(IllegalArgumentException::class.java) {
                service.earnPoints(customerId, 50000, null)
            }
        }

        @Test
        fun `earns points with transactionId`() {
            val customerId = UUID.randomUUID()
            val txId = UUID.randomUUID()
            val customer =
                CustomerEntity().apply {
                    id = customerId
                    organizationId = orgId
                    name = "Test"
                    points = 0
                }
            whenever(customerRepository.findById(customerId)).thenReturn(customer)

            val earned = service.earnPoints(customerId, 100000, txId)

            assertEquals(10L, earned)
            assertEquals(10, customer.points)
            verify(pointTransactionRepository).persist(any<PointTransactionEntity>())
        }
    }

    @Nested
    inner class RedeemPoints {
        @Test
        fun `throws when customer not found`() {
            val customerId = UUID.randomUUID()
            whenever(customerRepository.findByIdForUpdate(customerId)).thenReturn(null)

            assertThrows(IllegalArgumentException::class.java) {
                service.redeemPoints(customerId, 10, null)
            }
        }

        @Test
        fun `redeems with transactionId`() {
            val customerId = UUID.randomUUID()
            val txId = UUID.randomUUID()
            val customer =
                CustomerEntity().apply {
                    id = customerId
                    organizationId = orgId
                    name = "Test"
                    points = 100
                }
            whenever(customerRepository.findByIdForUpdate(customerId)).thenReturn(customer)

            val result = service.redeemPoints(customerId, 30, txId)

            assertEquals(true, result)
            assertEquals(70, customer.points)
            verify(pointTransactionRepository).persist(any<PointTransactionEntity>())
        }
    }
}
