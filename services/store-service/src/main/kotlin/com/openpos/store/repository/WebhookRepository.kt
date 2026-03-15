package com.openpos.store.repository

import com.openpos.store.entity.WebhookEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class WebhookRepository : PanacheRepositoryBase<WebhookEntity, UUID> {
    fun findActiveByOrganizationId(organizationId: UUID): List<WebhookEntity> =
        list("organizationId = ?1 AND isActive = true", organizationId)

    fun findByOrganizationId(organizationId: UUID): List<WebhookEntity> = list("organizationId = ?1", organizationId)
}
