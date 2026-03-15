package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.AttendanceEntity
import com.openpos.store.repository.AttendanceRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@ApplicationScoped
class AttendanceService {
    @Inject lateinit var attendanceRepository: AttendanceRepository
    @Inject lateinit var tenantFilterService: TenantFilterService
    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun clockIn(staffId: UUID, storeId: UUID): AttendanceEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val today = LocalDate.now()
        val entity = AttendanceEntity().apply {
            this.organizationId = orgId
            this.staffId = staffId
            this.storeId = storeId
            this.date = today
            this.clockIn = Instant.now()
        }
        attendanceRepository.persist(entity)
        return entity
    }

    @Transactional
    fun clockOut(staffId: UUID): AttendanceEntity? {
        tenantFilterService.enableFilter()
        val today = LocalDate.now()
        val entity = attendanceRepository.findByStaffAndDate(staffId, today) ?: return null
        entity.clockOut = Instant.now()
        attendanceRepository.persist(entity)
        return entity
    }

    fun listByStoreAndDate(storeId: UUID, date: LocalDate): List<AttendanceEntity> {
        tenantFilterService.enableFilter()
        return attendanceRepository.listByStoreAndDate(storeId, date)
    }
}
