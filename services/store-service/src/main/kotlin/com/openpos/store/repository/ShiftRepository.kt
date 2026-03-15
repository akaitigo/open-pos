package com.openpos.store.repository

import com.openpos.store.entity.ShiftEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class ShiftRepository : PanacheRepositoryBase<ShiftEntity, UUID> {
    fun listByStoreAndDate(storeId: UUID, date: LocalDate): List<ShiftEntity> =
        find("storeId = ?1 and date = ?2", Sort.ascending("startTime"), storeId, date).list()

    fun listByStaffAndDateRange(staffId: UUID, from: LocalDate, to: LocalDate): List<ShiftEntity> =
        find("staffId = ?1 and date >= ?2 and date <= ?3", Sort.ascending("date"), staffId, from, to).list()
}
