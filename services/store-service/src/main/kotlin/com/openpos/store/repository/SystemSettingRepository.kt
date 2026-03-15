package com.openpos.store.repository

import com.openpos.store.entity.SystemSettingEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import jakarta.enterprise.context.ApplicationScoped
import java.util.UUID

@ApplicationScoped
class SystemSettingRepository : PanacheRepositoryBase<SystemSettingEntity, UUID> {
    fun findByKey(key: String): SystemSettingEntity? = find("key = ?1", key).firstResult()

    fun listAllSorted(): List<SystemSettingEntity> =
        listAll(
            io.quarkus.panache.common.Sort
                .ascending("key"),
        )
}
