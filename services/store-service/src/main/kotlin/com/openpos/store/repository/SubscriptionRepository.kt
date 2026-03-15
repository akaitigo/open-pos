package com.openpos.store.repository

import com.openpos.store.entity.SubscriptionEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SubscriptionRepository : PanacheRepositoryBase<SubscriptionEntity, UUID> {
    fun findByOrganizationId(organizationId: UUID): List<SubscriptionEntity> = list("organizationId = ?1", organizationId)

    fun findActiveByOrganizationId(organizationId: UUID): SubscriptionEntity? =
        find("organizationId = ?1 AND status = 'ACTIVE'", organizationId).firstResult()
}
