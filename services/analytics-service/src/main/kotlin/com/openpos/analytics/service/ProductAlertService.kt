package com.openpos.analytics.service

import com.openpos.analytics.config.OrganizationIdHolder
import com.openpos.analytics.config.TenantFilterService
import com.openpos.analytics.entity.ProductAlertEntity
import com.openpos.analytics.repository.ProductAlertRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

/**
 * 商品アラートサービス。
 * 売れ筋変動・在庫異常の検出と通知を提供する。
 */
@ApplicationScoped
class ProductAlertService {
    @Inject
    lateinit var productAlertRepository: ProductAlertRepository

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    fun listByOrganization(
        organizationId: UUID,
        page: Int,
        pageSize: Int,
    ): Pair<List<ProductAlertEntity>, Long> {
        val panachePage = Page.of(page, pageSize)
        val alerts = productAlertRepository.findByOrganizationId(organizationId, panachePage)
        val count = productAlertRepository.countByOrganizationId(organizationId)
        return Pair(alerts, count)
    }

    fun listUnread(organizationId: UUID): List<ProductAlertEntity> = productAlertRepository.findUnreadByOrganizationId(organizationId)

    @Transactional
    fun create(
        organizationId: UUID,
        productId: UUID,
        alertType: String,
        description: String,
    ): ProductAlertEntity {
        val entity =
            ProductAlertEntity().apply {
                this.organizationId = organizationId
                this.productId = productId
                this.alertType = alertType
                this.description = description
            }
        productAlertRepository.persist(entity)
        return entity
    }

    @Transactional
    fun markAsRead(id: UUID): ProductAlertEntity? {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set in OrganizationIdHolder"
            }
        // ProductAlertEntity は BaseEntity を継承していないため Hibernate Filter が効かない。
        // HQL クエリで全テナントのレコードが返るため、手動でテナント所有権を検証する。
        val entity = productAlertRepository.find("id = ?1", id).firstResult() ?: return null
        // 手動でテナント所有権を検証し、他テナントのアラートを操作できないようにする。
        if (entity.organizationId != orgId) {
            return null
        }
        entity.isRead = true
        productAlertRepository.persist(entity)
        return entity
    }
}
