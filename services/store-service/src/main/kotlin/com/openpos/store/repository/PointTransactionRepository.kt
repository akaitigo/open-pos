package com.openpos.store.repository

import com.openpos.store.entity.PointTransactionEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Page
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class PointTransactionRepository : PanacheRepositoryBase<PointTransactionEntity, UUID> {
    fun listByCustomerId(customerId: UUID, page: Page): List<PointTransactionEntity> =
        find("customerId", Sort.descending("createdAt"), customerId).page(page).list()

    fun countByCustomerId(customerId: UUID): Long = count("customerId", customerId)
}
