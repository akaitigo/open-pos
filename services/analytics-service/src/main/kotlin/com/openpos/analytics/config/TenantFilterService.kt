package com.openpos.analytics.config

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.hibernate.Session

/**
 * Hibernate の organizationFilter を有効化するサービス。
 * OrganizationIdHolder に設定された organizationId を使って
 * 全クエリに organization_id 条件を自動付与する。
 */
@ApplicationScoped
class TenantFilterService {
    @Inject
    lateinit var entityManager: EntityManager

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    /**
     * organizationFilter を有効化する。
     * organizationId が未設定の場合は IllegalStateException をスローする。
     */
    fun enableFilter() {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set in OrganizationIdHolder"
            }
        entityManager
            .unwrap(Session::class.java)
            .enableFilter("organizationFilter")
            .setParameter("organizationId", orgId)
    }
}
