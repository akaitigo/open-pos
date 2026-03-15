package com.openpos.store.service

import com.openpos.store.entity.PlanEntity
import com.openpos.store.entity.SubscriptionEntity
import com.openpos.store.repository.PlanRepository
import com.openpos.store.repository.SubscriptionRepository
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
class PlanServiceTest {
    @Inject
    lateinit var planService: PlanService

    @InjectMock
    lateinit var planRepository: PlanRepository

    @InjectMock
    lateinit var subscriptionRepository: SubscriptionRepository

    @BeforeEach
    fun setUp() {
        doNothing().whenever(planRepository).persist(any<PlanEntity>())
        doNothing().whenever(subscriptionRepository).persist(any<SubscriptionEntity>())
    }

    @Test
    fun `createPlan should create plan with correct fields`() {
        val plan =
            planService.createPlan(
                name = "スターター",
                maxStores = 3,
                maxTerminals = 10,
                maxProducts = 1000,
                monthlyPrice = 298000,
            )

        assertNotNull(plan)
        assertEquals("スターター", plan.name)
        assertEquals(3, plan.maxStores)
        assertEquals(10, plan.maxTerminals)
        assertEquals(1000, plan.maxProducts)
        assertEquals(298000, plan.monthlyPrice)
    }

    @Test
    fun `listPlans should return active plans`() {
        val plan1 =
            PlanEntity().apply {
                id = UUID.randomUUID()
                name = "フリー"
                maxStores = 1
                maxTerminals = 2
                maxProducts = 100
                monthlyPrice = 0
            }
        whenever(planRepository.findActive()).thenReturn(listOf(plan1))

        val plans = planService.listPlans()

        assertEquals(1, plans.size)
        assertEquals("フリー", plans[0].name)
    }

    @Test
    fun `subscribe should create active subscription`() {
        val orgId = UUID.randomUUID()
        val planId = UUID.randomUUID()

        val subscription = planService.subscribe(orgId, planId)

        assertNotNull(subscription)
        assertEquals(orgId, subscription.organizationId)
        assertEquals(planId, subscription.planId)
        assertEquals("ACTIVE", subscription.status)
    }

    @Test
    fun `cancelSubscription should set status to CANCELLED`() {
        val subscriptionId = UUID.randomUUID()
        val entity =
            SubscriptionEntity().apply {
                id = subscriptionId
                organizationId = UUID.randomUUID()
                planId = UUID.randomUUID()
                status = "ACTIVE"
            }
        whenever(subscriptionRepository.findById(subscriptionId)).thenReturn(entity)

        val result = planService.cancelSubscription(subscriptionId)

        assertNotNull(result)
        assertEquals("CANCELLED", result?.status)
    }
}
