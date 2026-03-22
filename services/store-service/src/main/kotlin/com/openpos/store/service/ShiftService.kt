package com.openpos.store.service

import com.openpos.store.config.OrganizationIdHolder
import com.openpos.store.config.TenantFilterService
import com.openpos.store.entity.ShiftEntity
import com.openpos.store.repository.ShiftRepository
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

@Deprecated("v1.0未使用: gRPC RPC未接続")
@ApplicationScoped
class ShiftService {
    @Inject lateinit var shiftRepository: ShiftRepository
    @Inject lateinit var tenantFilterService: TenantFilterService
    @Inject lateinit var organizationIdHolder: OrganizationIdHolder

    @Transactional
    fun create(staffId: UUID, storeId: UUID, date: LocalDate, startTime: LocalTime, endTime: LocalTime, note: String?): ShiftEntity {
        val orgId = requireNotNull(organizationIdHolder.organizationId) { "organizationId is not set" }
        val entity = ShiftEntity().apply {
            this.organizationId = orgId
            this.staffId = staffId
            this.storeId = storeId
            this.date = date
            this.startTime = startTime
            this.endTime = endTime
            this.note = note
        }
        shiftRepository.persist(entity)
        return entity
    }

    fun listByStoreAndDate(storeId: UUID, date: LocalDate): List<ShiftEntity> {
        tenantFilterService.enableFilter()
        return shiftRepository.listByStoreAndDate(storeId, date)
    }

    @Transactional
    fun delete(id: UUID): Boolean {
        tenantFilterService.enableFilter()
        val entity = shiftRepository.findById(id) ?: return false
        shiftRepository.delete(entity)
        return true
    }
}
