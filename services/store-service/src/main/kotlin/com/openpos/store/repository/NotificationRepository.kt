package com.openpos.store.repository

import com.openpos.store.entity.NotificationEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class NotificationRepository : PanacheRepositoryBase<NotificationEntity, UUID> {
    fun listPaginated(page: Page): List<NotificationEntity> =
        findAll(Sort.descending("createdAt")).page(page).list()

    fun countUnread(): Long = count("isRead", false)
}
