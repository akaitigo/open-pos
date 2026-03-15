package com.openpos.store.service

import com.openpos.store.entity.PlanEntity
import com.openpos.store.entity.SubscriptionEntity
import com.openpos.store.repository.PlanRepository
import com.openpos.store.repository.SubscriptionRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID
import java.util.logging.Logger

/**
 * プラン・サブスクリプション管理サービス。
 * プランの CRUD とテナントのサブスクリプション管理を提供する。
 */
@ApplicationScoped
class PlanService {
    @Inject
    lateinit var planRepository: PlanRepository

    @Inject
    lateinit var subscriptionRepository: SubscriptionRepository

    companion object {
        private val logger: Logger = Logger.getLogger(PlanService::class.java.name)
    }

    fun listPlans(): List<PlanEntity> = planRepository.findActive()

    fun findPlanById(id: UUID): PlanEntity? = planRepository.findById(id)

    @Transactional
    fun createPlan(
        name: String,
        maxStores: Int,
        maxTerminals: Int,
        maxProducts: Int,
        monthlyPrice: Long,
    ): PlanEntity {
        val entity =
            PlanEntity().apply {
                this.name = name
                this.maxStores = maxStores
                this.maxTerminals = maxTerminals
                this.maxProducts = maxProducts
                this.monthlyPrice = monthlyPrice
            }
        planRepository.persist(entity)
        logger.info("Created plan: ${entity.name}")
        return entity
    }

    fun getSubscription(organizationId: UUID): SubscriptionEntity? = subscriptionRepository.findActiveByOrganizationId(organizationId)

    @Transactional
    fun subscribe(
        organizationId: UUID,
        planId: UUID,
    ): SubscriptionEntity {
        val entity =
            SubscriptionEntity().apply {
                this.organizationId = organizationId
                this.planId = planId
                this.status = "ACTIVE"
            }
        subscriptionRepository.persist(entity)
        logger.info("Organization $organizationId subscribed to plan $planId")
        return entity
    }

    @Transactional
    fun cancelSubscription(subscriptionId: UUID): SubscriptionEntity? {
        val entity = subscriptionRepository.findById(subscriptionId) ?: return null
        entity.status = "CANCELLED"
        subscriptionRepository.persist(entity)
        logger.info("Subscription $subscriptionId cancelled")
        return entity
    }
}
