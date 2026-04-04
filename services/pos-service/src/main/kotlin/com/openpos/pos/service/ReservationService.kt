package com.openpos.pos.service

import com.openpos.pos.config.OrganizationIdHolder
import com.openpos.pos.config.TenantFilterService
import com.openpos.pos.entity.ReservationEntity
import com.openpos.pos.repository.ReservationRepository
import io.quarkus.panache.common.Page
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import java.time.Instant
import java.util.UUID

/**
 * 予約注文サービス。
 * 取り置き・予約注文の作成、履行、キャンセルを提供する。
 */
@ApplicationScoped
class ReservationService {
    @Inject
    lateinit var reservationRepository: ReservationRepository

    @Inject
    lateinit var tenantFilterService: TenantFilterService

    @Inject
    lateinit var organizationIdHolder: OrganizationIdHolder

    fun findById(id: UUID): ReservationEntity? {
        tenantFilterService.enableFilter()
        // findById() は em.find() ベースのため Hibernate Filter をバイパスする。
        // HQL クエリで organizationFilter を適用してテナント隔離を保証する。
        return reservationRepository.find("id = ?1", id).firstResult()
    }

    fun listByStoreId(
        storeId: UUID,
        status: String?,
        page: Int,
        pageSize: Int,
    ): Pair<List<ReservationEntity>, Long> {
        tenantFilterService.enableFilter()
        val panachePage = Page.of(page, pageSize)
        return if (status != null) {
            val items = reservationRepository.listByStoreIdAndStatus(storeId, status, panachePage)
            val count = reservationRepository.countByStoreIdAndStatus(storeId, status)
            Pair(items, count)
        } else {
            val items = reservationRepository.listByStoreId(storeId, panachePage)
            val count = reservationRepository.countByStoreId(storeId)
            Pair(items, count)
        }
    }

    @Transactional
    fun create(
        storeId: UUID,
        customerName: String?,
        customerPhone: String?,
        items: String,
        reservedUntil: Instant,
        note: String?,
    ): ReservationEntity {
        val orgId =
            requireNotNull(organizationIdHolder.organizationId) {
                "organizationId is not set"
            }

        val entity =
            ReservationEntity().apply {
                this.organizationId = orgId
                this.storeId = storeId
                this.customerName = customerName
                this.customerPhone = customerPhone
                this.items = items
                this.reservedUntil = reservedUntil
                this.note = note
            }
        reservationRepository.persist(entity)
        return entity
    }

    @Transactional
    fun fulfill(id: UUID): ReservationEntity? {
        tenantFilterService.enableFilter()
        val entity = reservationRepository.find("id = ?1", id).firstResult() ?: return null
        entity.status = "FULFILLED"
        reservationRepository.persist(entity)
        return entity
    }

    @Transactional
    fun cancel(id: UUID): ReservationEntity? {
        tenantFilterService.enableFilter()
        val entity = reservationRepository.find("id = ?1", id).firstResult() ?: return null
        entity.status = "CANCELLED"
        reservationRepository.persist(entity)
        return entity
    }
}
