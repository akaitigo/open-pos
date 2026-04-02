package com.openpos.pos.repository

import com.openpos.pos.entity.DiscountReasonEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class DiscountReasonRepository : PanacheRepositoryBase<DiscountReasonEntity, UUID> {
    fun findActive(): List<DiscountReasonEntity> = list("isActive = true")

    fun findAllOrdered(): List<DiscountReasonEntity> = list("ORDER BY createdAt DESC")

    fun findByCode(code: String): DiscountReasonEntity? = find("code = ?1", code).firstResult()
}
