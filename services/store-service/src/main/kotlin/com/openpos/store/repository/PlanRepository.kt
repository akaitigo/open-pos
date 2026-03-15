package com.openpos.store.repository

import com.openpos.store.entity.PlanEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class PlanRepository : PanacheRepositoryBase<PlanEntity, UUID> {
    fun findActive(): List<PlanEntity> = list("isActive = true")
}
