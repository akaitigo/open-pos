package com.openpos.pos.repository

import com.openpos.pos.entity.DrawerEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class DrawerRepository : PanacheRepositoryBase<DrawerEntity, UUID> {
    fun findByTerminal(
        storeId: UUID,
        terminalId: UUID,
    ): DrawerEntity? =
        find("storeId = ?1 and terminalId = ?2 and isOpen = true", storeId, terminalId)
            .firstResult()

    fun findLatestByTerminal(
        storeId: UUID,
        terminalId: UUID,
    ): DrawerEntity? =
        find("storeId = ?1 and terminalId = ?2 order by createdAt desc", storeId, terminalId)
            .firstResult()
}
