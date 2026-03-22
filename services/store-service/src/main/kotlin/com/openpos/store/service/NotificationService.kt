package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.NotificationEntity
import com.openpos.store.repository.NotificationRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.util.UUID

@Deprecated("v1.0未使用: gRPC RPC未接続")
@ApplicationScoped
class NotificationService {
    @Inject lateinit var notificationRepository: NotificationRepository
    @Inject lateinit var tenantFilterService: TenantFilterService
    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(type: String, title: String, message: String): NotificationEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity = NotificationEntity().apply {
            this.organizationId = orgId
            this.type = type
            this.title = title
            this.message = message
        }
        notificationRepository.persist(entity)
        return entity
    }

    fun list(page: Int, pageSize: Int): Pair<List<NotificationEntity>, Long> {
        tenantFilterService.enableFilter()
        val items = notificationRepository.listPaginated(Page.of(page, pageSize))
        val total = notificationRepository.count()
        return Pair(items, total)
    }

    fun countUnread(): Long {
        tenantFilterService.enableFilter()
        return notificationRepository.countUnread()
    }

    @Transactional
    fun markAsRead(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = notificationRepository.findById(id) ?: return false
        entity.isRead = true
        notificationRepository.persist(entity)
        return true
    }
}
