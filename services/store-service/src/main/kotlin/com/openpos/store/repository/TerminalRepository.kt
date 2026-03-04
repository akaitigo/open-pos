package com.openpos.store.repository

import com.openpos.store.entity.TerminalEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class TerminalRepository : PanacheRepositoryBase<TerminalEntity, UUID> {
    fun findByStoreId(storeId: UUID): List<TerminalEntity> = list("storeId = ?1", Sort.ascending("terminalCode"), storeId)
}
