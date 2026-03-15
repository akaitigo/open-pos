package com.openpos.store.repository

import com.openpos.store.entity.AttendanceEntity
import io.quarkus.hibernate.orm.panache.kotlin.PanacheRepositoryBase
import io.quarkus.panache.common.Sort
import jakarta.enterprise.context.ApplicationScoped
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class AttendanceRepository : PanacheRepositoryBase<AttendanceEntity, UUID> {
    fun findByStaffAndDate(staffId: UUID, date: LocalDate): AttendanceEntity? =
        find("staffId = ?1 and date = ?2", staffId, date).firstResult()

    fun listByStoreAndDate(storeId: UUID, date: LocalDate): List<AttendanceEntity> =
        find("storeId = ?1 and date = ?2", Sort.ascending("clockIn"), storeId, date).list()
}
