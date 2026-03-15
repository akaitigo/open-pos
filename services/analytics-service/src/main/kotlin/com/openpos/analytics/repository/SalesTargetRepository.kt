package com.openpos.analytics.repository

import com.openpos.analytics.entity.SalesTargetEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class SalesTargetRepository : PanacheRepositoryBase<SalesTargetEntity, UUID> {
    fun findByStoreAndMonth(
        storeId: UUID?,
        targetMonth: LocalDate,
    ): SalesTargetEntity? =
        if (storeId != null) {
            find("storeId = ?1 AND targetMonth = ?2", storeId, targetMonth).firstResult()
        } else {
            find("storeId IS NULL AND targetMonth = ?1", targetMonth).firstResult()
        }

    fun listByOrganization(): List<SalesTargetEntity> = listAll()
}
