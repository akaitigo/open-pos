package com.openpos.store.service

import com.openpos.store.entity.PlanEntity
import com.openpos.store.entity.SubscriptionEntity
import com.openpos.store.repository.PlanRepository
import com.openpos.store.repository.SubscriptionRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.UUID

class PlanServiceUnitTest {
    private lateinit var service: PlanService
    private lateinit var planRepository: PlanRepository
    private lateinit var subscriptionRepository: SubscriptionRepository

    @BeforeEach
    fun setUp() {
        planRepository = mock()
        subscriptionRepository = mock()

        service = PlanService()
        service.planRepository = planRepository
        service.subscriptionRepository = subscriptionRepository

        doNothing().whenever(planRepository).persist(any<PlanEntity>())
        doNothing().whenever(subscriptionRepository).persist(any<SubscriptionEntity>())
    }

    @Nested
    inner class ListPlans {
        @Test
        fun `returns active plans`() {
            val plans =
                listOf(
                    PlanEntity().apply {
                        id = UUID.randomUUID()
                        name = "Free"
                        maxStores = 1
                        maxTerminals = 2
                        maxProducts = 100
                        monthlyPrice = 0
                    },
                )
            whenever(planRepository.findActive()).thenReturn(plans)

            val result = service.listPlans()

            assertEquals(1, result.size)
            assertEquals("Free", result[0].name)
        }
    }

    @Nested
    inner class FindPlanById {
        @Test
        fun `returns plan when found`() {
            val planId = UUID.randomUUID()
            val plan =
                PlanEntity().apply {
                    id = planId
                    name = "Pro"
                    maxStores = 10
                    maxTerminals = 50
                    maxProducts = 10000
                    monthlyPrice = 500000
                }
            whenever(planRepository.findById(planId)).thenReturn(plan)

            val result = service.findPlanById(planId)

            assertNotNull(result)
            assertEquals("Pro", result?.name)
        }

        @Test
        fun `returns null when not found`() {
            val planId = UUID.randomUUID()
            whenever(planRepository.findById(planId)).thenReturn(null)

            val result = service.findPlanById(planId)

            assertNull(result)
        }
    }

    @Nested
    inner class CreatePlan {
        @Test
        fun `creates plan with correct fields`() {
            val result = service.createPlan("Starter", 3, 10, 1000, 298000)

            assertEquals("Starter", result.name)
            assertEquals(3, result.maxStores)
            assertEquals(10, result.maxTerminals)
            assertEquals(1000, result.maxProducts)
            assertEquals(298000, result.monthlyPrice)
        }
    }

    @Nested
    inner class GetSubscription {
        @Test
        fun `returns subscription when found`() {
            val orgId = UUID.randomUUID()
            val sub =
                SubscriptionEntity().apply {
                    id = UUID.randomUUID()
                    organizationId = orgId
                    planId = UUID.randomUUID()
                    status = "ACTIVE"
                }
            whenever(subscriptionRepository.findActiveByOrganizationId(orgId)).thenReturn(sub)

            val result = service.getSubscription(orgId)

            assertNotNull(result)
            assertEquals("ACTIVE", result?.status)
        }
    }

    @Nested
    inner class Subscribe {
        @Test
        fun `creates active subscription`() {
            val orgId = UUID.randomUUID()
            val planId = UUID.randomUUID()

            val result = service.subscribe(orgId, planId)

            assertEquals(orgId, result.organizationId)
            assertEquals(planId, result.planId)
            assertEquals("ACTIVE", result.status)
        }
    }

    @Nested
    inner class CancelSubscription {
        @Test
        fun `cancels subscription`() {
            val subId = UUID.randomUUID()
            val entity =
                SubscriptionEntity().apply {
                    id = subId
                    organizationId = UUID.randomUUID()
                    planId = UUID.randomUUID()
                    status = "ACTIVE"
                }
            whenever(subscriptionRepository.findById(subId)).thenReturn(entity)

            val result = service.cancelSubscription(subId)

            assertNotNull(result)
            assertEquals("CANCELLED", result?.status)
        }

        @Test
        fun `returns null when subscription not found`() {
            val subId = UUID.randomUUID()
            whenever(subscriptionRepository.findById(subId)).thenReturn(null)

            val result = service.cancelSubscription(subId)

            assertNull(result)
        }
    }
}
