package com.openpos.store.repository

import com.openpos.store.entity.StampCardEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class StampCardRepository : PanacheRepositoryBase<StampCardEntity, UUID> {
    fun findActiveByCustomerId(customerId: UUID): StampCardEntity? = find("customerId = ?1 AND status = 'ACTIVE'", customerId).firstResult()

    fun findByCustomerId(customerId: UUID): StampCardEntity? = find("customerId = ?1 ORDER BY createdAt DESC", customerId).firstResult()
}
