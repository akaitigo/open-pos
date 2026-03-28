package com.openpos.pos.repository

import com.openpos.pos.entity.GiftCardEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class GiftCardRepository : PanacheRepositoryBase<GiftCardEntity, UUID> {
    fun findByCode(code: String): GiftCardEntity? = find("code", code).firstResult()

    fun listAllOrdered(): List<GiftCardEntity> = list("ORDER BY createdAt DESC")
}
