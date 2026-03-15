package com.openpos.product.repository

import com.openpos.product.entity.TimeSaleEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.Instant
import java.util.UUID

@ApplicationScoped
class TimeSaleRepository : PanacheRepositoryBase<TimeSaleEntity, UUID> {
    fun findActiveByProduct(productId: UUID, now: Instant): TimeSaleEntity? =
        find("productId = ?1 and isActive = true and startTime <= ?2 and endTime >= ?2", productId, now).firstResult()

    fun listActiveNow(now: Instant): List<TimeSaleEntity> =
        find("isActive = true and startTime <= ?1 and endTime >= ?1", now).list()
}
