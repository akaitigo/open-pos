package com.openpos.store.repository

import com.openpos.store.entity.GiftCardEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import jakarta.persistence.LockModeType
import java.util.UUID

@ApplicationScoped
class GiftCardRepository : PanacheRepositoryBase<GiftCardEntity, UUID> {
    fun findByCode(code: String): GiftCardEntity? = find("code", code).firstResult()

    fun findByCodeForUpdate(code: String): GiftCardEntity? = find("code", code).withLock(LockModeType.PESSIMISTIC_WRITE).firstResult()
}
